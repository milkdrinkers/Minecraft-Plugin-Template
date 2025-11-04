package io.github.exampleuser.exampleplugin.utility;

import io.github.exampleuser.exampleplugin.messaging.MessagingHandler;
import io.github.exampleuser.exampleplugin.messaging.broker.BrokerType;
import io.github.exampleuser.exampleplugin.messaging.message.OutgoingMessage;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.CompletableFuture;

/**
 * Convenience class for accessing methods in {@link MessagingHandler}
 */
public final class Messaging {
    private static Messaging INSTANCE;

    private static Messaging getInstance() {
        if (INSTANCE == null)
            INSTANCE = new Messaging();
        return INSTANCE;
    }

    private MessagingHandler messagingHandler;

    private MessagingHandler getMessagingHandler() {
        return messagingHandler;
    }

    private void setMessengerHandler(MessagingHandler handler) {
        this.messagingHandler = handler;
    }

    /**
     * Used to set the globally used messaging handler instance for the plugin
     */
    @ApiStatus.Internal
    public static void init(MessagingHandler handler) {
        getInstance().setMessengerHandler(handler);
    }

    /**
     * Convenience method for {@link MessagingHandler#isReady()}
     *
     * @return if the message broker is ready
     */
    public static boolean isReady() {
        final MessagingHandler handler = getInstance().getMessagingHandler();
        if (handler == null)
            return false;

        return handler.isReady();
    }

    /**
     * Convenience method for accessing the {@link MessagingHandler} instance
     *
     * @return the messaging handler
     */
    @NotNull
    public static MessagingHandler getHandler() {
        return getInstance().getMessagingHandler();
    }

    /**
     * Convenience method for {@link MessagingHandler#getType()} to get {@link BrokerType}
     *
     * @return the broker type, defaults to {@link BrokerType#DATABASE} if not loaded
     */
    public static BrokerType getType() {
        return getInstance().getMessagingHandler().getType();
    }

    /**
     * Convenience method for {@link MessagingHandler#send(OutgoingMessage)}
     *
     * @param message the outgoing message
     * @return if the message was successfully sent
     */
    public static <T> CompletableFuture<Boolean> send(final OutgoingMessage<T> message) {
        final MessagingHandler handler = getInstance().getMessagingHandler();
        if (handler == null)
            return CompletableFuture.completedFuture(false);

        return handler.send(message);
    }
}
