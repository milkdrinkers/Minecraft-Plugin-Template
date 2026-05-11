package io.github.exampleuser.example.config.migration;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.configurate.CommentedConfigurationNode;
import org.spongepowered.configurate.ConfigurateException;
import org.spongepowered.configurate.ConfigurationNode;
import org.spongepowered.configurate.NodePath;
import org.spongepowered.configurate.serialize.SerializationException;
import org.spongepowered.configurate.transformation.ConfigurationTransformation;
import org.spongepowered.configurate.transformation.TransformAction;

import java.util.ArrayList;
import java.util.List;

/**
 * A single migration applied when upgrading a config from one version to the next.
 *
 * <p>Construct migrations via {@link #builder()} and register them in your config's
 * {@code migrations()} override, keyed by the <em>target</em> version they produce:
 * <pre>{@code
 * @Override
 * public Map<Integer, Migration> migrations() {
 *     return Map.of(
 *         2, Migration.builder()
 *             .rename(NodePath.path("old-section"), "newSection")
 *             .delete(NodePath.path("legacy-key"))
 *             .build()
 *     );
 * }
 * }</pre>
 *
 * <p>Since {@code Migration} implements {@link ConfigurationTransformation}, it integrates
 * directly with Configurate's versioned transformation machinery.
 */
public final class Migration implements ConfigurationTransformation {
    private final ConfigurationTransformation inner;

    private Migration(@NotNull ConfigurationTransformation inner) {
        this.inner = inner;
    }

    @Override
    public void apply(@NotNull ConfigurationNode node) throws ConfigurateException {
        inner.apply(node);
    }

    /**
     * Returns a new step-by-step builder for constructing a migration.
     *
     * @return a fresh {@link Builder}
     */
    @Contract(pure = true)
    @NotNull
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Fluent builder for a {@link Migration}.
     *
     * <p>Operations are applied in the order they are declared. Adjacent path-based calls
     * ({@link #rename}, {@link #move}, {@link #relocate}, {@link #delete}, {@link #setValue},
     * {@link #withAction}) are batched into a single Configurate traversal pass for efficiency.
     * Calling {@link #copy} or {@link #withTransform} flushes the current batch first,
     * since those operations need direct access to the full node tree.
     */
    public static final class Builder {
        private final List<ConfigurationTransformation> steps = new ArrayList<>();
        private @Nullable ConfigurationTransformation.Builder pathBuilder;

        Builder() {
        }

        /**
         * Rename the last key segment of a node in-place, keeping it under the same parent.
         *
         * <pre>{@code .rename(NodePath.path("update-checker"), "updateChecker")}</pre>
         *
         * @param path   the current path of the node
         * @param newKey replacement for the last segment
         * @return this builder
         */
        @NotNull
        public Builder rename(@NotNull NodePath path, @NotNull String newKey) {
            pathBuilder().addAction(path, (p, value) -> {
                final Object[] arr = p.array();
                if (arr.length == 0) return null;
                final Object[] out = arr.clone();
                out[out.length - 1] = newKey;
                return out;
            });
            return this;
        }

        /**
         * Move a node under a new parent section, preserving its original key name.
         *
         * <p>Example, move {@code language} into {@code settings}, producing {@code settings.language}:
         * <pre>{@code .move(NodePath.path("language"), "settings")}</pre>
         *
         * @param path      the current path of the node
         * @param newParent the parent path to nest the node under
         * @return this builder
         */
        @NotNull
        public Builder move(@NotNull NodePath path, @NotNull String... newParent) {
            pathBuilder().addAction(path, MigrationUtil.moveTo(newParent));
            return this;
        }

        /**
         * Move a node to an entirely new path, replacing both its parent and key name.
         *
         * <p>Example, move {@code old-key} to {@code section.new-key}:
         * <pre>{@code .relocate(NodePath.path("old-key"), "section", "new-key")}</pre>
         *
         * @param from    the current path of the node
         * @param newPath the full replacement path segments
         * @return this builder
         */
        @NotNull
        public Builder relocate(@NotNull NodePath from, @NotNull String... newPath) {
            pathBuilder().addAction(from, MigrationUtil.renameAndMove(newPath));
            return this;
        }

        /**
         * Remove a node from the config entirely.
         *
         * <pre>{@code .delete(NodePath.path("removed-key"))}</pre>
         *
         * @param path the path of the node to remove
         * @return this builder
         */
        @NotNull
        public Builder delete(@NotNull NodePath path) {
            pathBuilder().addAction(path, TransformAction.remove());
            return this;
        }

        /**
         * Copy the value of one node to another path, leaving the original intact.
         * The YAML comment on the source node is preserved at the destination.
         *
         * <p>This flushes the current batch of path-based operations before executing,
         * since it requires access to the full node tree.
         *
         * @param from source path to copy from
         * @param to   destination path to copy into
         * @return this builder
         * @see #copy(NodePath, NodePath, boolean)
         */
        @NotNull
        public Builder copy(@NotNull NodePath from, @NotNull NodePath to) {
            return copy(from, to, true);
        }

        /**
         * Copy the value of one node to another path, leaving the original intact.
         *
         * <p>This flushes the current batch of path-based operations before executing,
         * since it requires access to the full node tree.
         *
         * @param from            source path to copy from
         * @param to              destination path to copy into
         * @param preserveComment {@code true} to also copy the YAML comment from the source node
         *                        to the destination; {@code false} to copy the value only
         * @return this builder
         */
        @NotNull
        public Builder copy(@NotNull NodePath from, @NotNull NodePath to, boolean preserveComment) {
            flush();
            steps.add(root -> {
                try {
                    root.node(to).set(root.node(from).raw());
                    if (preserveComment && root instanceof CommentedConfigurationNode commented) {
                        final String comment = commented.node(from).comment();
                        if (comment != null && !comment.isEmpty()) {
                            commented.node(to).comment(comment);
                        }
                    }
                } catch (SerializationException e) {
                    throw new RuntimeException("Failed to copy node " + from + " to " + to, e);
                }
            });
            return this;
        }

        /**
         * Set a node to a fixed value, overwriting whatever was previously there.
         * Handy for injecting defaults for keys newly introduced in this version.
         *
         * <pre>{@code .setValue(NodePath.path("settings", "newFlag"), true)}</pre>
         *
         * @param path  the path to set
         * @param value the value to assign; {@code null} clears the node
         * @return this builder
         */
        @NotNull
        public Builder setValue(@NotNull NodePath path, @Nullable Object value) {
            pathBuilder().addAction(path, MigrationUtil.setValue(value));
            return this;
        }

        /**
         * Low-level escape hatch, attach a raw {@link TransformAction} to a specific path.
         * Use this when the fluent methods don't cover your use case but you still want
         * path-matched semantics.
         *
         * <p>The action receives the matched node and should return:
         * <ul>
         *   <li>the new path as {@code Object[]} to move the node</li>
         *   <li>{@code null} to keep the current path</li>
         *   <li>{@link TransformAction#remove()} to delete the node</li>
         * </ul>
         *
         * @param path   the path to match
         * @param action the action to run at that path
         * @return this builder
         */
        @NotNull
        public Builder withAction(@NotNull NodePath path, @NotNull TransformAction action) {
            pathBuilder().addAction(path, action);
            return this;
        }

        /**
         * Full-tree escape hatch, apply a {@link ConfigurationTransformation} that receives
         * the root node directly and can freely read or write anywhere in the tree.
         *
         * <p>This flushes any pending path-based operations before the transform is added.
         *
         * @param transform the transformation to apply to the root node
         * @return this builder
         */
        @NotNull
        public Builder withTransform(@NotNull ConfigurationTransformation transform) {
            flush();
            steps.add(transform);
            return this;
        }

        /**
         * Builds the {@link Migration} from the accumulated steps.
         *
         * @return the assembled migration
         */
        @NotNull
        public Migration build() {
            flush();
            if (steps.isEmpty()) return new Migration(node -> {
            });
            if (steps.size() == 1) return new Migration(steps.getFirst());
            return new Migration(ConfigurationTransformation.chain(steps.toArray(new ConfigurationTransformation[0])));
        }

        private @NotNull ConfigurationTransformation.Builder pathBuilder() {
            if (pathBuilder == null) pathBuilder = ConfigurationTransformation.builder();
            return pathBuilder;
        }

        private void flush() {
            if (pathBuilder == null) return;
            steps.add(pathBuilder.build());
            pathBuilder = null;
        }
    }
}
