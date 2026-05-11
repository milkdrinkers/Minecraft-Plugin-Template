package io.github.exampleuser.example.config.typeserializer;

import org.jetbrains.annotations.Nullable;
import org.spongepowered.configurate.ConfigurationNode;
import org.spongepowered.configurate.serialize.SerializationException;
import org.spongepowered.configurate.serialize.TypeSerializer;

import java.lang.reflect.Type;
import java.time.Duration;

public final class DurationSerializer implements TypeSerializer<Duration> {
    public static final DurationSerializer INSTANCE = new DurationSerializer();

    @Override
    public Duration deserialize(Type type, ConfigurationNode node) throws SerializationException {
        final long value = node.getLong();
        return Duration.ofMillis(value);
    }

    @Override
    public void serialize(Type type, @Nullable Duration obj, ConfigurationNode node) throws SerializationException {
        if (obj == null) {
            node.raw(null);
            return;
        }

        node.set(obj.toMillis());
    }
}
