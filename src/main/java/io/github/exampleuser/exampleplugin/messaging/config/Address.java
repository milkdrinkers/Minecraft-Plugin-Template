package io.github.exampleuser.exampleplugin.messaging.config;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;

/**
 * Represents a connection address for a message broker.
 *
 * @param host the host address (never null)
 * @param port the port (optional)
 */
@SuppressWarnings("unused")
public record Address(@NotNull String host, @Nullable Integer port) {
    private static final int MIN_PORT = 1;
    private static final int MAX_PORT = 65535;

    public Address {
        if (host.trim().isEmpty())
            throw new IllegalArgumentException("Host cannot be null or empty");
        host = host.trim();

        if (port != null && (port < MIN_PORT || port > MAX_PORT))
            throw new IllegalArgumentException("Port must be between " + MIN_PORT + " and " + MAX_PORT);
    }

    /**
     * Parse a string into an Address.
     *
     * @param address the address string to parse (format: "host" or "host:port")
     * @return Address instance
     * @throws IllegalArgumentException if the address format is invalid
     */
    public static @NotNull Address parse(@Nullable String address) {
        if (address == null || address.trim().isEmpty())
            return new Address(AddressList.DEFAULT_HOST, null);

        address = address.trim();
        final int colonIndex = address.lastIndexOf(':');

        if (colonIndex == -1)
            return new Address(address, null);

        final String host = address.substring(0, colonIndex);
        final String portStr = address.substring(colonIndex + 1);

        if (host.isEmpty())
            throw new IllegalArgumentException("Host cannot be empty in address: " + address);

        // Trailing colon, no port
        if (portStr.isEmpty())
            return new Address(host, null);

        try {
            return new Address(host, Integer.parseInt(portStr));
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Invalid port number in address: " + address, e);
        }
    }

    /**
     * Get the port as an Optional.
     *
     * @return Optional containing the port, or empty if no port is set
     */
    public @NotNull Optional<Integer> getPort() {
        return Optional.ofNullable(port);
    }

    /**
     * Format this address as a connection string.
     *
     * @return formatted address string
     */
    public @NotNull String toConnectionString() {
        return port != null ? host + ":" + port : host;
    }

    @Override
    public @NotNull String toString() {
        return toConnectionString();
    }
}
