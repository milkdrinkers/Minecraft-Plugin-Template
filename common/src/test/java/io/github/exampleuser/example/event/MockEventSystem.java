package io.github.exampleuser.example.event;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@SuppressWarnings("unused")
public class MockEventSystem {
    private static final Logger LOGGER = LoggerFactory.getLogger(MockEventSystem.class);
    private static final List<MockEventListener> listeners = Collections.synchronizedList(new ArrayList<>());

    public static void registerListener(MockEventListener listener) {
        listeners.add(listener);
        LOGGER.debug("Registered listener");
    }

    public static void unregisterListener(MockEventListener listener) {
        listeners.remove(listener);
        LOGGER.debug("Unregistered listener");
    }

    public static void fireEvent(MockEvent event) {
        LOGGER.debug("Firing event of type: {}", event.getClass().getSimpleName());

        if (listeners.isEmpty())
            LOGGER.warn("No listeners found when firing event of type: {}", event.getClass().getSimpleName());

        listeners.forEach(listener -> listener.onEvent(event));
    }

    public static void clear() {
        final int count = listeners.size();
        listeners.clear();
        LOGGER.debug("Unregistered {} listeners", count);
    }
}