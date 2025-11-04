package io.github.exampleuser.exampleplugin.messaging.broker;

import org.jetbrains.annotations.Nullable;

/**
 * Represents message broker implementations supported by this plugin.
 */
public enum BrokerType {
    DATABASE("sql"),
    PLUGIN_MESSAGING("plugin"),
    REDIS("redis"),
    RABBITMQ("rabbitmq"),
    NATS("nats");

    private final String name;

    BrokerType(String name) {
        this.name = name;
    }

    /**
     * The name of this broker type
     *
     * @return the name
     */
    public String getName() {
        return name;
    }

    /**
     * Get a broker type from a broker name.
     *
     * @param name broker name
     * @return broker type or null if none exist by that name
     */
    public static @Nullable BrokerType fromName(String name) {
        for (BrokerType type : values()) {
            if (type.name.equalsIgnoreCase(name)) {
                return type;
            }
        }
        return null;
    }
}
