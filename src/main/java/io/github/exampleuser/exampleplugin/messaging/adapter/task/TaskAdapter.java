package io.github.exampleuser.exampleplugin.messaging.adapter.task;

import java.util.concurrent.TimeUnit;

/**
 * Represents a platform independent task runner used by the message brokers for common tasks (like doing keepalive checks).
 */
public interface TaskAdapter {
    void init(Runnable runnable, long delay, long interval, TimeUnit timeUnit);

    default void init(Runnable runnable, long delay, long interval) {
        init(runnable, delay, interval, TimeUnit.SECONDS);
    }

    default void init(Runnable runnable, long delay) {
        init(runnable, delay, 5L);
    }

    default void init(Runnable runnable) {
        init(runnable, 0L);
    }

    void cancel();
}
