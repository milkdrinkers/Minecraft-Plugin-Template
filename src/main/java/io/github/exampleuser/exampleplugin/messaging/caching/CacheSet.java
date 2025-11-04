package io.github.exampleuser.exampleplugin.messaging.caching;

import java.util.Set;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

/**
 * A Set wrapper evicting all entries if the Set hasn't been modified in a certain amount of time.
 *
 * @param <T> the type to store
 */
@SuppressWarnings("unused")
public class CacheSet<T> implements AutoCloseable {
    private static final ForkJoinPool pool = new ForkJoinPool();
    private static final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1, r -> {
        final Thread t = new Thread(r, "CacheSet-Scheduler");
        t.setDaemon(true);
        return t;
    });

    private final Set<T> cache;
    private final long evictionDelayMillis;
    private final AtomicLong lastModified;
    private final AtomicReference<ScheduledFuture<?>> evictionTask;

    public CacheSet(long evictionDelay, TimeUnit timeUnit) {
        this.cache = ConcurrentHashMap.newKeySet();
        this.evictionDelayMillis = timeUnit.toMillis(evictionDelay);
        this.lastModified = new AtomicLong(System.currentTimeMillis());
        this.evictionTask = new AtomicReference<>();
        scheduleEviction();
    }

    public boolean add(T value) {
        updateLastModified();
        return cache.add(value);
    }

    public boolean remove(T value) {
        updateLastModified();
        return cache.remove(value);
    }

    public boolean contains(T value) {
        return cache.contains(value);
    }

    public void clear() {
        updateLastModified();
        cache.clear();
    }

    public int size() {
        return cache.size();
    }

    public boolean isEmpty() {
        return cache.isEmpty();
    }

    private void updateLastModified() {
        lastModified.set(System.currentTimeMillis());
        scheduleEviction();
    }

    private void scheduleEviction() {
        final ScheduledFuture<?> currentTask = evictionTask.getAndSet(null);
        if (currentTask != null)
            currentTask.cancel(false);

        final ScheduledFuture<?> newTask = scheduler.schedule(() -> pool.execute(() -> { // Use pool to execute work
            long timeSinceLastModified = System.currentTimeMillis() - lastModified.get();
            if (timeSinceLastModified >= evictionDelayMillis) { // Evict
                cache.clear();
            } else { // Reschedule for remaining time
                final long remainingTime = evictionDelayMillis - timeSinceLastModified;
                final ScheduledFuture<?> rescheduleTask = scheduler.schedule(() -> pool.execute(this::checkAndEvict), remainingTime, TimeUnit.MILLISECONDS);
                evictionTask.set(rescheduleTask);
            }
        }), evictionDelayMillis, TimeUnit.MILLISECONDS);

        evictionTask.set(newTask);
    }

    private void checkAndEvict() {
        final long timeSinceLastModified = System.currentTimeMillis() - lastModified.get();
        if (timeSinceLastModified >= evictionDelayMillis)
            cache.clear();
    }

    @Override
    public void close() {
        final ScheduledFuture<?> currentTask = evictionTask.getAndSet(null);
        if (currentTask != null)
            currentTask.cancel(false);
    }
}
