package io.github.exampleuser.example.config;

import io.github.exampleuser.example.config.exception.ConfigValidationException;
import io.github.exampleuser.example.config.migration.Migration;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Unmodifiable;
import org.spongepowered.configurate.interfaces.meta.Exclude;
import org.spongepowered.configurate.objectmapping.meta.Comment;
import org.spongepowered.configurate.transformation.ConfigurationTransformation;

import java.util.Map;

/**
 * Base interface for all versioned configuration files.
 *
 * <p>Implement {@link #migrations()} to declare version-to-version migrations,
 * and optionally override {@link #validate()} to enforce constraints on loaded values.
 *
 * <p>Minimal example:
 * <pre>{@code
 * @ConfigSerializable
 * public class MyConfig implements VersionedConfig {
 *     public int configVersion = 1;
 *
 *     @Override public int configVersion() { return configVersion; }
 *
 *     @Override
 *     public Map<Integer, Migration> migrations() {
 *         return Map.of(
 *             2, Migration.builder()
 *                 .rename(NodePath.path("old-key"), "newKey")
 *                 .delete(NodePath.path("removed-key"))
 *                 .build()
 *         );
 *     }
 *
 *     @Override
 *     public void validate() throws ConfigValidationException {
 *         if (someValue < 0) throw new ConfigValidationException("someValue must be >= 0");
 *     }
 * }
 * }</pre>
 */
public interface VersionedConfig {

    /**
     * The current version stored in the config file. Configurate uses this to determine
     * which migrations need to be applied when loading an older file.
     *
     * <p>Implementations must declare a {@code public int configVersion} field <em>with that
     * exact name</em> and return it here. Configurate serializes this field as {@code config-version}
     * in YAML (kebab-case), and the default {@link #migrator()} uses that same key to track
     * versions on disk. Start versioning at {@code 1}; version {@code 0} and negative values
     * are treated as "no version" by Configurate's versioned transformer.
     *
     * @return the version number read from disk
     */
    @Comment("Do not change this value!")
    @SuppressWarnings("unused")
    int configVersion();

    /**
     * Declares the migrations used to upgrade older config files to newer versions.
     *
     * <p>Each entry maps a <em>target</em> version to the {@link Migration} that produces it.
     * Migrations are applied in ascending version order, so a file at version 1 receiving
     * migrations for versions 2 and 3 will have both applied in sequence.
     *
     * <p>Keep each migration self-contained and scoped to the config class it lives in,
     * that keeps the history easy to follow as the format evolves.
     *
     * <p>Example:
     * <pre>{@code
     * @Override
     * public Map<Integer, Migration> migrations() {
     *     return Map.of(
     *         2, Migration.builder()
     *             .rename(NodePath.path("update-checker"), "updateChecker")
     *             .build(),
     *         3, Migration.builder()
     *             .delete(NodePath.path("deprecated-flag"))
     *             .move(NodePath.path("language"), "settings")
     *             .build()
     *     );
     * }
     * }</pre>
     *
     * @return an unmodifiable map of target version → migration; empty by default
     */
    @Exclude
    @NotNull
    @Unmodifiable
    default Map<Integer, Migration> migrations() {
        return Map.of();
    }

    /**
     * Builds the versioned migrator used by {@link io.github.exampleuser.example.config.loading.ConfigLoader}
     * to upgrade on-disk nodes before deserialization.
     *
     * <p>The version key is hard-coded to {@code "config-version"}, which is the kebab-case
     * form Configurate writes for a field named {@code configVersion}. Override this method
     * only if you need a different key; otherwise override {@link #migrations()} instead.
     *
     * @return a {@link ConfigurationTransformation.Versioned} assembled from {@link #migrations()}
     */
    @Exclude
    @NotNull
    default ConfigurationTransformation.Versioned migrator() {
        final ConfigurationTransformation.VersionedBuilder builder = ConfigurationTransformation.versionedBuilder()
            .versionKey("config-version");

        for (Map.Entry<Integer, Migration> entry : migrations().entrySet()) {
            builder.addVersion(entry.getKey(), entry.getValue());
        }

        return builder.build();
    }

    /**
     * Validates the config after loading. Throw {@link ConfigValidationException} for any
     * field value that is unacceptable, the loader will propagate it and treat the config
     * as failed.
     *
     * @throws ConfigValidationException if any field value is invalid
     */
    @Exclude
    default void validate() throws ConfigValidationException {
    }
}
