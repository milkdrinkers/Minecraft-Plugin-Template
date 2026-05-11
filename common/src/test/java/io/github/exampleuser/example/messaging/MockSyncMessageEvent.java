package io.github.exampleuser.example.messaging;

import io.github.exampleuser.example.event.MockEvent;
import io.github.exampleuser.example.messaging.message.Message;

public class MockSyncMessageEvent extends MockEvent {
    private final Message<?> message;

    public MockSyncMessageEvent(Message<?> message) {
        this.message = message;
    }

    public Message<?> getMessage() {
        return message;
    }
}