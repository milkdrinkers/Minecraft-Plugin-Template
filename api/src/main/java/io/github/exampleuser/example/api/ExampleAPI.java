package io.github.exampleuser.example.api;

import org.jetbrains.annotations.ApiStatus;

/**
 * The ExampleAPI class is the main entry point for accessing the Example API.
 */
public abstract class ExampleAPI {
    private static ExampleAPI INSTANCE;

    /**
     * Gets the instance of the ExampleAPI.
     *
     * @return the instance of ExampleAPI
     * @since 1.0.0
     */
    public static ExampleAPI getInstance() {
        if (INSTANCE == null)
            throw new RuntimeException("API was accessed before being initialized!");
        return INSTANCE;
    }

    /**
     * Sets the instance of the ExampleAPI.
     * This method is intended for internal use by the api provider only.
     *
     * @param api the instance of ExampleAPI to set
     * @since 1.0.0
     */
    @ApiStatus.Internal
    protected static void setInstance(ExampleAPI api) {
        INSTANCE = api;
    }
}
