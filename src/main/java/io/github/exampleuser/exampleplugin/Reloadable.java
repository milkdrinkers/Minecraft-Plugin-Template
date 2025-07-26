package io.github.exampleuser.exampleplugin;

/**
 * Implemented in classes that should support being reloaded IE executing the methods during runtime after startup.
 */
public interface Reloadable {
    /**
     * On plugin load.
     */
    default void onLoad(ExamplePlugin plugin) {};

    /**
     * On plugin enable.
     */
    default void onEnable(ExamplePlugin plugin) {}

    /**
     * On plugin disable.
     */
    default void onDisable(ExamplePlugin plugin) {};
}
