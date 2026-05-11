package io.github.exampleuser.example;

/**
 * Defines the common lifecycle methods and constraints for a service.
 *
 * <p>Services implementing this interface follow a simple lifecycle:
 * <ol>
 *   <li>Initial state: stopped</li>
 *   <li>Call {@link #doStartup()} to transition to started state</li>
 *   <li>Call {@link #doShutdown()} to transition back to stopped state</li>
 * </ol>
 *
 * <p>This interface extends {@link AutoCloseable} to support try-with-resources usage.
 */
public interface ServiceLifecycle extends AutoCloseable {
    /**
     * Starts the service, transitioning it from stopped to started state.
     *
     * @throws RuntimeException      if startup fails
     * @throws IllegalStateException if the service is already started or in transition
     * @implSpec The service and its components are in an operational state if this method executes successfully.
     */
    void doStartup() throws RuntimeException;

    /**
     * Shuts down the service, transitioning it from started to stopped state.
     *
     * @throws RuntimeException      if shutdown fails
     * @throws IllegalStateException if the service is already stopped or in transition
     * @implSpec The service and its components are in a non-operational state if this method executes successfully.
     * @implNote This method does not execute if the service is already shut down.
     */
    default void doShutdown() throws RuntimeException {
        try {
            close();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Returns {@code true} if the service is currently started and operational.
     *
     * @return {@code true} if started, {@code false} otherwise
     */
    boolean isStarted();

    /**
     * Returns {@code true} if the service is currently stopped.
     *
     * @return {@code true} if stopped, {@code false} otherwise
     */
    boolean isShutdown();

    /**
     * Returns {@code true} if the service is currently in the process of starting.
     *
     * @return {@code true} if starting, {@code false} otherwise
     */
    boolean isStarting();

    /**
     * Returns {@code true} if the service is currently in the process of shutting down.
     *
     * @return {@code true} if shutting down, {@code false} otherwise
     */
    boolean isShuttingDown();

    /**
     * Shuts down the service as part of the {@link AutoCloseable} contract.
     * This method wraps any exceptions in {@link RuntimeException}.
     *
     * @throws RuntimeException if shutdown fails
     */
    @Override
    default void close() throws RuntimeException {
    }
}
