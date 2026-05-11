package io.github.exampleuser.example.config.typeserializer;

import org.jetbrains.annotations.Nullable;
import org.spongepowered.configurate.ConfigurationNode;
import org.spongepowered.configurate.serialize.SerializationException;
import org.spongepowered.configurate.serialize.TypeSerializer;

import java.lang.reflect.Type;
import java.math.BigDecimal;

public final class BigDecimalSerializer implements TypeSerializer<BigDecimal> {
    public static final BigDecimalSerializer INSTANCE = new BigDecimalSerializer();

    @Override
    public BigDecimal deserialize(Type type, ConfigurationNode node) throws SerializationException {
        final double value = node.getDouble();
        return BigDecimal.valueOf(value);
    }

    @Override
    public void serialize(Type type, @Nullable BigDecimal obj, ConfigurationNode node) throws SerializationException {
        if (obj == null) {
            node.raw(null);
            return;
        }

        node.set(obj.doubleValue());
    }
}
