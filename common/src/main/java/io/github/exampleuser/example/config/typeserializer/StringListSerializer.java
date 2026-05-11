package io.github.exampleuser.example.config.typeserializer;

import io.leangen.geantyref.TypeToken;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.configurate.ConfigurationNode;
import org.spongepowered.configurate.serialize.SerializationException;
import org.spongepowered.configurate.serialize.TypeSerializer;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.List;

/**
 * Serializer for {@code List<String>} that accepts either a plain scalar string or a YAML list.
 *
 * <p>A scalar value like {@code addresses: localhost:6379} is treated as a single-element list,
 * while a YAML sequence is read as multiple entries. This lets users write a compact single-address
 * config without switching to list syntax. Serialization always writes a YAML list for consistency.
 */
public final class StringListSerializer implements TypeSerializer<List<String>> {
    public static final StringListSerializer INSTANCE = new StringListSerializer();

    public static final TypeToken<List<String>> TYPE_TOKEN = new TypeToken<>() {
        @Override
        public Type getType() {
            return new ParameterizedType() {
                @Override
                public Type @NotNull [] getActualTypeArguments() {
                    return new Type[]{String.class};
                }

                @Override
                public @NotNull Type getRawType() {
                    return List.class;
                }

                @Override
                public Type getOwnerType() {
                    return null;
                }
            };
        }
    };

    @Override
    public List<String> deserialize(Type type, ConfigurationNode node) throws SerializationException {
        if (node.isList()) {
            return node.childrenList().stream()
                .map(n -> n.getString(""))
                .filter(s -> !s.isEmpty())
                .toList();
        }

        final String scalar = node.getString();
        if (scalar != null && !scalar.isEmpty())
            return List.of(scalar);

        return List.of();
    }

    @Override
    public void serialize(Type type, @Nullable List<String> obj, ConfigurationNode node) throws SerializationException {
        if (obj == null || obj.isEmpty()) {
            node.raw(null);
            return;
        }

        if (obj.size() == 1) {
            node.raw(obj.getFirst());
            return;
        }

        node.raw(null);
        for (final String value : obj) {
            node.appendListNode().raw(value);
        }
    }
}
