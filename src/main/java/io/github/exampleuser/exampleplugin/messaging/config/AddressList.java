package io.github.exampleuser.exampleplugin.messaging.config;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * Represents a list of broker addresses for single or cluster connections.
 */
public final class AddressList {
    static final String DEFAULT_HOST = "localhost";
    private final List<Address> addresses;

    private AddressList(@NotNull List<Address> addresses) {
        if (addresses.isEmpty())
            throw new IllegalArgumentException("Address list cannot be empty");
        this.addresses = List.copyOf(addresses);
    }

    /**
     * Create an AddressList from various input types.
     *
     * @param input can be {@code String}, {@code Collection<String>}, or {@code Collection<Address>}
     * @return AddressList instance
     * @throws IllegalArgumentException if input type is unsupported or invalid
     */
    @SuppressWarnings("unchecked")
    public static @NotNull AddressList from(@Nullable Object input) {
        switch (input) {
            case null -> {
                return single(DEFAULT_HOST);
            }
            case String stringAddress -> {
                return single(stringAddress);
            }
            case Address address -> {
                return new AddressList(List.of(address));
            }
            case Collection<?> collection -> {
                if (collection.isEmpty())
                    return single(DEFAULT_HOST);

                // Handle Collection<String>
                if (collection.iterator().next() instanceof String)
                    return fromStringCollection((Collection<String>) collection);

                // Handle Collection<Address>
                if (collection.iterator().next() instanceof Address)
                    return new AddressList(new ArrayList<>((Collection<Address>) collection));
            }
            default -> {
            }
        }

        throw new IllegalArgumentException("Unsupported input type: " + input.getClass().getSimpleName());
    }

    /**
     * Create a single-address list.
     *
     * @param address the address string
     * @return AddressList with one address
     */
    public static @NotNull AddressList single(@NotNull String address) {
        return new AddressList(List.of(Address.parse(address)));
    }

    /**
     * Create an AddressList from multiple address strings.
     *
     * @param addresses collection of address strings
     * @return AddressList instance
     */
    public static @NotNull AddressList fromStrings(@NotNull Collection<String> addresses) {
        return fromStringCollection(addresses);
    }

    private static @NotNull AddressList fromStringCollection(@NotNull Collection<String> addresses) {
        final List<Address> parsedAddresses = addresses.stream()
            .filter(Objects::nonNull)
            .map(String::trim)
            .filter(s -> !s.isEmpty())
            .map(Address::parse)
            .collect(Collectors.toList());

        if (parsedAddresses.isEmpty())
            return single(DEFAULT_HOST);

        return new AddressList(parsedAddresses);
    }

    /**
     * Check if this is a single address configuration.
     *
     * @return true if only one address is configured
     */
    public boolean isSingle() {
        return addresses.size() == 1;
    }

    /**
     * Check if this is a cluster configuration (multiple addresses).
     *
     * @return true if multiple addresses are configured
     */
    public boolean isCluster() {
        return addresses.size() > 1;
    }

    /**
     * Get the first address (useful for single-address configurations).
     *
     * @return the first address
     */
    public @NotNull Address getFirst() {
        return addresses.getFirst();
    }

    /**
     * Get all addresses as an immutable list.
     *
     * @return immutable list of addresses
     */
    public @NotNull List<Address> getAll() {
        return addresses;
    }

    /**
     * Get the number of addresses.
     *
     * @return address count
     */
    public int size() {
        return addresses.size();
    }

    /**
     * Format all addresses as connection strings.
     *
     * @return list of connection strings
     */
    public @NotNull List<String> toConnectionStrings() {
        return addresses.stream()
            .map(Address::toConnectionString)
            .collect(Collectors.toList());
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        AddressList that = (AddressList) o;
        return Objects.equals(addresses, that.addresses);
    }

    @Override
    public int hashCode() {
        return Objects.hash(addresses);
    }

    @Override
    public @NotNull String toString() {
        if (isSingle()) {
            return getFirst().toString();
        }
        return addresses.stream()
            .map(Address::toString)
            .collect(Collectors.joining(", ", "[", "]"));
    }
}
