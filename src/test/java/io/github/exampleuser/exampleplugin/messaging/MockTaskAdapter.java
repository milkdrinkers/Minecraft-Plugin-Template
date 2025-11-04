package io.github.exampleuser.exampleplugin.messaging;

import io.github.exampleuser.exampleplugin.messaging.adapter.task.TaskAdapter;

import java.util.Map;
import java.util.concurrent.*;

public class MockTaskAdapter implements TaskAdapter {
    private final ScheduledExecutorService executor = Executors.newScheduledThreadPool(10);
    private final Map<Runnable, ScheduledFuture<?>> scheduledTasks = new ConcurrentHashMap<>();

    @Override
    public void init(Runnable runnable, long delay, long interval, TimeUnit timeUnit) {
        if (executor.isShutdown())
            throw new RuntimeException("Attempted to schedule runnable while executor is shut down!");

        final ScheduledFuture<?> task = executor.scheduleAtFixedRate(
            runnable,
            delay,
            interval,
            timeUnit
        );

        scheduledTasks.put(runnable, task);
    }

    @Override
    public void cancel() {
        for (final ScheduledFuture<?> task : scheduledTasks.values()) {
            if (task != null && !task.isCancelled())
                task.cancel(false);
        }

        scheduledTasks.clear();

        if (!executor.isShutdown())
            executor.shutdown();
    }
}
