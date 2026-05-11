package io.github.exampleuser.example;

/**
 * Implemented in classes that should support being reloaded IE executing the methods during runtime after startup.
 */
public interface Reloadable {
    /**
     * On plugin load.
     */
    default void onLoad(AbstractExample plugin) {
    }

    /**
     * On plugin enable.
     */
    default void onEnable(AbstractExample plugin) {
    }

    /**
     * On plugin disable.
     */
    default void onDisable(AbstractExample plugin) {
    }

}
