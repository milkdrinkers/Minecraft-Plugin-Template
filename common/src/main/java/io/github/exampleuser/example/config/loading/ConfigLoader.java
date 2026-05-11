package io.github.exampleuser.example.config.loading;

import io.github.exampleuser.example.config.VersionedConfig;
import io.github.exampleuser.example.config.exception.ConfigValidationException;
import io.github.exampleuser.example.config.typeserializer.BigDecimalSerializer;
import io.github.exampleuser.example.config.typeserializer.DurationSerializer;
import io.github.exampleuser.example.config.typeserializer.LowercaseEnumSerializer;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.VisibleForTesting;
import org.slf4j.Logger;
import org.spongepowered.configurate.CommentedConfigurationNode;
import org.spongepowered.configurate.ConfigurateException;
import org.spongepowered.configurate.interfaces.InterfaceDefaultOptions;
import org.spongepowered.configurate.serialize.TypeSerializerCollection;
import org.spongepowered.configurate.transformation.ConfigurationTransformation;
import org.spongepowered.configurate.yaml.NodeStyle;
import org.spongepowered.configurate.yaml.YamlConfigurationLoader;

import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * Fluent builder that loads, migrates, and validates a {@link VersionedConfig} from a YAML file.
 *
 * <p>Typical usage:
 * <pre>{@code
 * MyConfig cfg = new ConfigLoader()
 *     .withLogger(logger)
 *     .withDirectory()
 *     .withPath(dataFolder.resolve("config.yml"))
 *     .withHeader("My Plugin Configuration")
 *     .build(MyConfig.class);
 * }</pre>
 *
 * <p>Use {@link #buildOrThrow(Class)} at plugin startup where a bad config should halt
 * initialization rather than silently returning {@code null}.
 * The raw node tree is accessible via {@link #getNode()} after a successful load.
 */
public class ConfigLoader {
    private @Nullable Consumer<CommentedConfigurationNode> transformer;
    private final List<Consumer<TypeSerializerCollection.Builder>> extraSerializers = new ArrayList<>();
    private @Nullable File file;
    private @NotNull String header = "";
    private @NotNull NodeStyle nodeStyle = NodeStyle.BLOCK;
    private int indent = 2;
    private boolean createDirectory = false;
    private @Nullable Logger logger;

    @VisibleForTesting
    @Nullable CommentedConfigurationNode configurationNode;

    @VisibleForTesting
    ConfigLoader(@NotNull Path path) {
        this.file = path.toFile();
    }

    public ConfigLoader() {
    }

    /**
     * Instructs the loader to create the parent directory of the config file if it does not exist.
     *
     * @return this loader
     */
    @Contract("-> this")
    @NotNull
    public ConfigLoader withDirectory() {
        createDirectory = true;
        return this;
    }

    /**
     * Sets the path to the YAML config file.
     *
     * @param path path to the file; the file does not need to exist yet
     * @return this loader
     */
    @Contract("_ -> this")
    @NotNull
    public ConfigLoader withPath(@NotNull Path path) {
        this.file = path.toFile();
        return this;
    }

    /**
     * Sets the config file. Equivalent to calling {@link #withPath(Path)} via {@link File#toPath()}.
     *
     * @param file the YAML file
     * @return this loader
     */
    @Contract("_ -> this")
    @NotNull
    public ConfigLoader withFile(@NotNull File file) {
        return withPath(file.toPath());
    }

    /**
     * Sets the block comment written at the very top of the YAML file.
     *
     * @param header the header text; may contain newlines
     * @return this loader
     */
    @Contract("_ -> this")
    @NotNull
    public ConfigLoader withHeader(@NotNull String header) {
        this.header = header;
        return this;
    }

    /**
     * Registers an additional type serializer for use during loading and saving.
     *
     * <p>Example:
     * <pre>{@code
     * .withSerializer(b -> b.registerExact(MyType.class, MyTypeSerializer.INSTANCE))
     * }</pre>
     *
     * @param serializer a consumer that registers one or more serializers on the provided builder
     * @return this loader
     */
    @Contract("_ -> this")
    @NotNull
    public ConfigLoader withSerializer(@NotNull Consumer<TypeSerializerCollection.Builder> serializer) {
        extraSerializers.add(serializer);
        return this;
    }

    /**
     * Registers a post-load transformer that runs against the root node after migrations
     * and after the file has been saved back to disk.
     *
     * <p>Changes made by the transformer are intentionally <em>not</em> persisted, this is
     * meant for injecting runtime-only values such as derived fields or environment overrides
     * that should never appear in the config file itself.
     *
     * @param transformer a consumer that receives and may modify the root node
     * @return this loader
     */
    @Contract("_ -> this")
    @NotNull
    public ConfigLoader withTransformer(@NotNull Consumer<CommentedConfigurationNode> transformer) {
        this.transformer = transformer;
        return this;
    }

    /**
     * Overrides the YAML node style used when writing the file.
     * Defaults to {@link NodeStyle#BLOCK}.
     *
     * @param nodeStyle the node style to apply
     * @return this loader
     */
    @Contract("_ -> this")
    @NotNull
    public ConfigLoader withNodeStyle(@NotNull NodeStyle nodeStyle) {
        this.nodeStyle = nodeStyle;
        return this;
    }

    /**
     * Overrides the YAML indentation width.
     * Defaults to {@code 2} spaces.
     *
     * @param indent number of spaces per indentation level; must be positive
     * @return this loader
     */
    @Contract("_ -> this")
    @NotNull
    public ConfigLoader withIndent(int indent) {
        this.indent = indent;
        return this;
    }

    /**
     * Sets the logger used for error and warning output during loading.
     *
     * @param logger the SLF4J logger to write to
     * @return this loader
     */
    @Contract("_ -> this")
    @NotNull
    public ConfigLoader withLogger(@NotNull Logger logger) {
        this.logger = logger;
        return this;
    }

    /**
     * Loads, migrates, and validates the config file, returning {@code null} on failure.
     *
     * <p>Both IO errors and validation failures are caught and logged at {@code ERROR} level
     * (if a logger is set), and {@code null} is returned in both cases. To distinguish
     * between the two, or to let the exception propagate, use {@link #buildOrThrow(Class)}.
     *
     * @param configClass the config class to deserialize into
     * @param <T>         a type implementing {@link VersionedConfig}
     * @return the loaded config instance, or {@code null} if loading failed
     */
    @Nullable
    public <T extends VersionedConfig> T build(@NotNull Class<T> configClass) {
        try {
            return loadInternal(configClass);
        } catch (ConfigValidationException e) {
            if (logger != null)
                logger.error("Config validation failed for {}: {}", configClass.getSimpleName(), e.getMessage());
            return null;
        } catch (IOException e) {
            if (logger != null)
                logger.error("Failed to load config {}", configClass.getSimpleName(), e);
            return null;
        }
    }

    /**
     * Loads, migrates, and validates the config file, throwing on failure instead of returning null.
     *
     * <p>Prefer this at plugin startup where a broken config should halt initialization.
     *
     * @param configClass the config class to deserialize into
     * @param <T>         a type implementing {@link VersionedConfig}
     * @return the loaded config instance, never {@code null}
     * @throws ConfigValidationException if the loaded values fail validation
     * @throws IOException               if the file cannot be read or deserialized
     */
    @NotNull
    public <T extends VersionedConfig> T buildOrThrow(@NotNull Class<T> configClass) throws IOException {
        return loadInternal(configClass);
    }

    /**
     * Returns the raw {@link CommentedConfigurationNode} from the most recent successful
     * {@link #build} or {@link #buildOrThrow} call, or {@code null} if no load has occurred yet.
     *
     * <p>Use this when strongly-typed config fields are not enough, for example, to read
     * a dynamic sub-section by key at runtime without declaring it as a field.
     *
     * @return the root configuration node, or {@code null}
     */
    @Nullable
    public CommentedConfigurationNode getNode() {
        return configurationNode;
    }

    @VisibleForTesting
    @NotNull
    CommentedConfigurationNode loadConfigurationNode(@NotNull Class<? extends VersionedConfig> configClass) throws ConfigurateException {
        loadInternal(configClass);
        //noinspection DataFlowIssue, loadInternal always sets configurationNode before returning
        return configurationNode.copy();
    }

    @NotNull
    private <T extends VersionedConfig> T loadInternal(@NotNull Class<T> configClass) throws ConfigurateException {
        if (file == null)
            throw new ConfigurateException("No config file path set, call withPath() or withFile() before build()");

        if (createDirectory && !file.getParentFile().exists()) {
            if (!file.getParentFile().mkdirs()) {
                if (logger != null)
                    logger.error("Failed to create directory for config file at {}", file.getAbsolutePath());
            }
        }

        final YamlConfigurationLoader loader = createLoader(file);
        final CommentedConfigurationNode node = loader.load();
        final boolean originallyEmpty = !file.exists() || node.isNull();

        // Apply migrations using a blank instance to obtain the migrator.
        // Skipped gracefully if there are no migrations defined, or if the class
        // cannot be instantiated (e.g. interfaces).
        int currentVersion = -1;
        int newVersion = -1;
        try {
            final T blank = configClass.getDeclaredConstructor().newInstance();
            if (!blank.migrations().isEmpty()) {
                final ConfigurationTransformation.Versioned migrator = blank.migrator();
                currentVersion = migrator.version(node);
                migrator.apply(node);
                newVersion = migrator.version(node);
            }
        } catch (ReflectiveOperationException e) {
            if (logger != null)
                logger.warn("Could not instantiate {} to apply migrations, skipping", configClass.getSimpleName());
        }

        T config = node.get(configClass);
        if (config == null)
            throw new ConfigurateException("Deserialization returned null for " + configClass.getSimpleName());

        // Re-serialize into a fresh root to enforce strict field ordering.
        // (Updating the old node in-place would push new fields to the bottom.)
        final CommentedConfigurationNode newRoot = CommentedConfigurationNode.root(loader.defaultOptions());
        newRoot.set(config);

        if (originallyEmpty || currentVersion != newVersion) {
            loader.save(newRoot);
        }

        // Transformer runs after saving so its changes are intentionally not persisted.
        if (transformer != null) {
            transformer.accept(newRoot);
            config = newRoot.get(configClass);
            if (config == null)
                throw new ConfigurateException("Deserialization returned null after transformer for " + configClass.getSimpleName());
        }

        configurationNode = newRoot;

        config.validate();

        return config;
    }

    @NotNull
    private YamlConfigurationLoader createLoader(@NotNull File file) {
        final TypeSerializerCollection.Builder serializerBuilder = TypeSerializerCollection.defaults().childBuilder()
            .register(LowercaseEnumSerializer.INSTANCE)
            .registerExact(BigDecimal.class, BigDecimalSerializer.INSTANCE)
            .registerExact(Duration.class, DurationSerializer.INSTANCE);

        for (Consumer<TypeSerializerCollection.Builder> extra : extraSerializers) {
            extra.accept(serializerBuilder);
        }

        return YamlConfigurationLoader.builder()
            .file(file)
            .indent(indent)
            .nodeStyle(nodeStyle)
            .defaultOptions(options -> InterfaceDefaultOptions.addTo(options, builder -> {
                    })
                    .shouldCopyDefaults(false)
                    .header(header)
                    .serializers(builder -> builder.registerAll(serializerBuilder.build()))
            )
            .build();
    }
}
