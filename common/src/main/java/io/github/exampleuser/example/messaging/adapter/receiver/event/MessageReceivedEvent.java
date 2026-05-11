package io.github.exampleuser.example.messaging.adapter.receiver.event;

import io.github.exampleuser.example.messaging.message.Message;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;

/**
 * Fired on the main thread when a message arrives from the broker.
 *
 * @implNote This event will never fire during {@link JavaPlugin#onLoad()} or {@link JavaPlugin#onDisable()}.
 */
@SuppressWarnings("unused")
public class MessageReceivedEvent extends Event {
    private static final HandlerList HANDLER_LIST = new HandlerList();
    private final Message<?> message;

    public MessageReceivedEvent(final Message<?> message) {
        this.message = message;
    }

    public Message<?> getMessage() {
        return message;
    }

    public static HandlerList getHandlerList() {
        return HANDLER_LIST;
    }

    @Override
    public @NotNull HandlerList getHandlers() {
        return HANDLER_LIST;
    }
}