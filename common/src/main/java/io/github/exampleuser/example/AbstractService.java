package io.github.exampleuser.example;

import java.util.concurrent.atomic.AtomicReference;

/**
 * Abstract base implementation of {@link ServiceLifecycle} that provides thread-safe
 * state management and lifecycle coordination.
 *
 * <p>This implementation uses template methods {@link #startup()} and {@link #shutdown()}
 * that subclasses must implement to provide their specific initialization and cleanup logic.
 * The state transitions are handled automatically by this base class.
 *
 * <p>State transitions are atomic and thread-safe. Only one thread can initiate a state
 * transition at a time, and all other threads will see consistent state during transitions.
 *
 * <p>Example usage:
 * <pre>{@code
 * public class MyService extends AbstractService {
 *     private DatabaseConnection connection;
 *
 *     @Override
 *     protected void startup() throws Exception {
 *         connection = new DatabaseConnection();
 *         connection.connect();
 *     }
 *
 *     @Override
 *     protected void shutdown() throws Exception {
 *         if (connection != null) {
 *             connection.disconnect();
 *         }
 *     }
 * }
 * }</pre>
 */
public abstract class AbstractService implements ServiceLifecycle {
    /**
     * Possible service states.
     */
    public enum State {
        /**
         * Service is stopped and not operational
         */
        STOPPED,
        /**
         * Service is in the process of starting
         */
        STARTING,
        /**
         * Service is started and operational
         */
        STARTED,
        /**
         * Service is in the process of shutting down
         */
        SHUTTING_DOWN
    }

    private final AtomicReference<State> state = new AtomicReference<>(State.STOPPED);

    /**
     * Gets the current state of the service.
     *
     * @return the current state
     */
    public State getState() {
        return state.get();
    }

    @Override
    public boolean isStarted() {
        return state.get() == State.STARTED;
    }

    @Override
    public boolean isShutdown() {
        return state.get() == State.STOPPED;
    }

    @Override
    public boolean isStarting() {
        return state.get() == State.STARTING;
    }

    @Override
    public boolean isShuttingDown() {
        return state.get() == State.SHUTTING_DOWN;
    }

    /**
     * Performs the actual startup logic for this service.
     * This method is called by the framework after the state has been
     * transitioned to {@link State#STARTING}.
     *
     * @throws Exception if startup fails
     */
    protected abstract void startup() throws Exception;

    /**
     * Performs the actual shutdown logic for this service.
     * This method is called by the framework after the state has been
     * transitioned to {@link State#SHUTTING_DOWN}.
     *
     * @throws Exception if shutdown fails
     */
    protected abstract void shutdown() throws Exception;

    @Override
    public final void doStartup() throws RuntimeException {
        if (!state.compareAndSet(State.STOPPED, State.STARTING)) {
            final State currentState = state.get();
            if (currentState == State.STARTED)
                return; // Already started

            throw new IllegalStateException("Cannot start service in state: " + currentState);
        }

        try {
            startService();
            if (!state.compareAndSet(State.STARTING, State.STARTED))
                throw new IllegalStateException("State was modified during startup");
        } catch (Exception e) {
            state.set(State.STOPPED);
            throw new RuntimeException("Service startup failed", e);
        }
    }

    @Override
    public final void doShutdown() throws RuntimeException {
        if (!state.compareAndSet(State.STARTED, State.SHUTTING_DOWN)) {
            final State currentState = state.get();
            if (currentState == State.STOPPED)
                return; // Already stopped

            throw new IllegalStateException("Cannot shutdown service in state: " + currentState);
        }

        try {
            stopService();
            if (!state.compareAndSet(State.SHUTTING_DOWN, State.STOPPED))
                throw new IllegalStateException("State was modified during shutdown");
        } catch (Exception e) {
            state.set(State.STOPPED);
            throw new RuntimeException("Service shutdown failed", e);
        }
    }

    /**
     * Internal method that delegates to the subclass's startup implementation.
     * This method is called by the framework after state transition.
     *
     * @throws Exception if startup fails
     */
    private void startService() throws Exception {
        startup();
    }

    /**
     * Internal method that delegates to the subclass's shutdown implementation.
     * This method is called by the framework after state transition.
     *
     * @throws Exception if shutdown fails
     */
    private void stopService() throws Exception {
        shutdown();
        close();
    }
}
