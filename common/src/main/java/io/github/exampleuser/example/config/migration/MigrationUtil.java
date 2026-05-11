package io.github.exampleuser.example.config.migration;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.configurate.ConfigurateException;
import org.spongepowered.configurate.transformation.TransformAction;

import java.util.Arrays;

/**
 * Static helpers for constructing {@link TransformAction}s.
 *
 * <p>These are primarily intended as arguments to {@link Migration.Builder#withAction},
 * the low-level escape hatch on the migration builder. The most common operations
 * (rename, move, relocate, delete, copy, setValue) are already covered by the builder's
 * own fluent methods and don't require reaching in here.
 *
 * <p>Portions derived from
 * <a href="https://github.com/GeyserMC/Geyser">GeyserMC/Geyser</a>
 * © 2024 GeyserMC, licensed under the MIT License.
 */
public final class MigrationUtil {
    private MigrationUtil() {
    }

    /**
     * Rename the last key segment of a path in-place, keeping the node under the same parent.
     *
     * @param newKey the replacement key name
     * @return a {@link TransformAction} that renames the last segment
     */
    @NotNull
    public static TransformAction rename(@NotNull String newKey) {
        return (path, value) -> {
            final Object[] arr = path.array();
            if (arr.length == 0) return null;
            final Object[] out = arr.clone();
            out[out.length - 1] = newKey;
            return out;
        };
    }

    /**
     * Move a node to an entirely new path, discarding the original key name.
     *
     * <p>Example, move {@code old-key} to {@code section.new-key}:
     * <pre>{@code MigrationUtil.renameAndMove("section", "new-key") }</pre>
     *
     * @param newPath the full destination path segments
     * @return a {@link TransformAction} that replaces the entire path
     */
    @NotNull
    public static TransformAction renameAndMove(@NotNull String... newPath) {
        return (path, value) -> Arrays.stream(newPath).toArray();
    }

    /**
     * Move a node under a new parent section, preserving its original key as the last segment.
     *
     * <p>Example, move {@code old-key} under {@code section}, producing {@code section.old-key}:
     * <pre>{@code MigrationUtil.moveTo("section") }</pre>
     *
     * @param newPath parent path segments to prepend
     * @return a {@link TransformAction} that nests the node under the new parent
     */
    @NotNull
    public static TransformAction moveTo(@NotNull String... newPath) {
        return (path, value) -> {
            final Object[] arr = path.array();
            if (arr.length == 0) {
                throw new ConfigurateException(value, "The root node cannot be moved!");
            }

            final Object[] result = new Object[newPath.length + 1];
            System.arraycopy(newPath, 0, result, 0, newPath.length);
            result[newPath.length] = arr[arr.length - 1];
            return result;
        };
    }

    /**
     * Set the matched node to a fixed value, overwriting whatever was there before.
     * Useful for injecting defaults for keys that are new in this version.
     *
     * @param value the value to assign; {@code null} clears the node
     * @return a {@link TransformAction} that overwrites the node's value and keeps its path
     */
    @NotNull
    public static TransformAction setValue(@Nullable Object value) {
        return (path, node) -> {
            node.set(value);
            return null;
        };
    }
}
