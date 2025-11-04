package io.github.exampleuser.exampleplugin.messaging.broker.database;

import io.github.exampleuser.exampleplugin.database.Queries;
import io.github.exampleuser.exampleplugin.messaging.MessageConsumer;
import io.github.exampleuser.exampleplugin.messaging.adapter.task.TaskAdapter;
import io.github.exampleuser.exampleplugin.messaging.broker.AbstractBroker;
import io.github.exampleuser.exampleplugin.messaging.config.MessagingConfig;
import io.github.exampleuser.exampleplugin.messaging.message.IncomingMessage;
import io.github.exampleuser.exampleplugin.messaging.message.OutgoingMessage;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * Implementation of a database as a message broker
 */
@SuppressWarnings({"FieldCanBeLocal", "unused"})
public final class DatabaseBroker extends AbstractBroker {
    private final String name;
    private final String channelName;
    private final ReentrantReadWriteLock lock = new ReentrantReadWriteLock(); // Used to prevent writing to database while reading

    private final TaskAdapter syncTask;
    private final TaskAdapter cleanupTask;

    private final AtomicInteger latestSyncId = new AtomicInteger(-1); // Tracks the last read message id to prevent re-reading messages
    private MessagingConfig config;

    public DatabaseBroker(MessageConsumer messageConsumer, String name, TaskAdapter syncTask, TaskAdapter cleanupTask) {
        super(messageConsumer);
        this.name = name;
        this.channelName = "%s:message".formatted(name.toLowerCase());
        this.syncTask = syncTask;
        this.cleanupTask = cleanupTask;
    }

    @Override
    public <T> void send(@NotNull OutgoingMessage<T> message) {
        try {
            lock.writeLock().lock();
            Queries.Sync.send(message);
        } finally {
            lock.writeLock().unlock();
        }
    }

    @Override
    public void init(MessagingConfig config) throws IOException, InterruptedException, NoSuchAlgorithmException {
        this.config = config;
        latestSyncId.set(Queries.Sync.fetchLatestMessageId().orElse(-1));
    }

    @Override
    public void enable(MessagingConfig config) throws IOException, InterruptedException, NoSuchAlgorithmException {
        syncTask.init(this::fetch, config.pollingInterval(), config.pollingInterval(), TimeUnit.MILLISECONDS);
        cleanupTask.init(this::cleanup, config.cleanupInterval(), config.cleanupInterval(), TimeUnit.MILLISECONDS);
    }

    @Override
    public void close() {
        if (syncTask != null)
            syncTask.cancel();

        if (cleanupTask != null)
            cleanupTask.cancel();

        latestSyncId.set(-1);
    }

    private void fetch() {
        try {
            lock.readLock().lock();
            final int oldId = latestSyncId.get();
            final Map<Integer, IncomingMessage<?, ?>> messages = Queries.Sync.receive(oldId, config.cleanupInterval());

            // Consume messages and update the latest id
            int newId = oldId;
            for (Map.Entry<Integer, IncomingMessage<?, ?>> message : messages.entrySet()) {
                final int messageId = message.getKey();
                newId = Math.max(newId, messageId);
                getMessageConsumer().consumeMessage(message.getValue());
            }

            latestSyncId.set(newId);
        } finally {
            lock.readLock().unlock();
        }
    }

    private void cleanup() {
        try {
            lock.readLock().lock();
            Queries.Sync.cleanup(config.cleanupInterval());
        } finally {
            lock.readLock().unlock();
        }
    }
}
