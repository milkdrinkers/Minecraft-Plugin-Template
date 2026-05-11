package io.github.exampleuser.example.event;

@FunctionalInterface
public interface MockEventListener {
    void onEvent(MockEvent event);
}