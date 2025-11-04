package io.github.exampleuser.exampleplugin.messaging.adapter.receiver;

import io.github.exampleuser.exampleplugin.messaging.adapter.receiver.event.MessageReceivedEvent;
import io.github.exampleuser.exampleplugin.messaging.message.IncomingMessage;
import io.github.milkdrinkers.threadutil.Scheduler;

public class BukkitReceiverAdapter extends ReceiverAdapter {
    @Override
    public void accept(IncomingMessage<?, ?> message) {
        Scheduler.sync(() -> {
            new MessageReceivedEvent(message).callEvent();
        }).execute();
    }
}
