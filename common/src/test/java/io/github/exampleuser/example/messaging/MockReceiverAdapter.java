package io.github.exampleuser.example.messaging;

import io.github.exampleuser.example.event.MockEventSystem;
import io.github.exampleuser.example.messaging.adapter.receiver.ReceiverAdapter;
import io.github.exampleuser.example.messaging.message.Message;

public class MockReceiverAdapter extends ReceiverAdapter {
    @Override
    public void accept(Message<?> message) {
        MockEventSystem.fireEvent(new MockSyncMessageEvent(message));
    }
}
