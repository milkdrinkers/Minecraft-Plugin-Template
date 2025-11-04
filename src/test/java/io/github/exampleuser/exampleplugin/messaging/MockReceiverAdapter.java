package io.github.exampleuser.exampleplugin.messaging;

import io.github.exampleuser.exampleplugin.event.MockEventSystem;
import io.github.exampleuser.exampleplugin.messaging.adapter.receiver.ReceiverAdapter;
import io.github.exampleuser.exampleplugin.messaging.message.IncomingMessage;

public class MockReceiverAdapter extends ReceiverAdapter {
    @Override
    public void accept(IncomingMessage<?, ?> message) {
        MockEventSystem.fireEvent(new MockSyncMessageEvent(message));
    }
}
