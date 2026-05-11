package io.github.exampleuser.example.config.typeserializer;

import io.leangen.geantyref.TypeToken;
import org.spongepowered.configurate.serialize.ScalarSerializer;
import org.spongepowered.configurate.serialize.Scalars;
import org.spongepowered.configurate.serialize.SerializationException;

import java.lang.reflect.Type;
import java.util.function.Predicate;

/**
 * Ensures enum values are written to lowercase.
 * {@link Scalars#ENUM} will read enum values in any case.
 *
 * <p>© 2024 GeyserMC, <a href="https://github.com/GeyserMC/Geyser">GeyserMC/Geyser</a>, MIT License.
 */
public final class LowercaseEnumSerializer extends ScalarSerializer<Enum<?>> {
    public static final LowercaseEnumSerializer INSTANCE = new LowercaseEnumSerializer();

    public LowercaseEnumSerializer() {
        super(new TypeToken<>() {
        });
    }

    @Override
    public Enum<?> deserialize(Type type, Object obj) throws SerializationException {
        return Scalars.ENUM.deserialize(type, obj);
    }

    @Override
    protected Object serialize(Enum<?> item, Predicate<Class<?>> typeSupported) {
        return item.name().toLowerCase();
    }
}
