package io.github.exampleuser.example.config.typeserializer;

import io.leangen.geantyref.TypeToken;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.configurate.ConfigurationNode;
import org.spongepowered.configurate.serialize.SerializationException;
import org.spongepowered.configurate.serialize.TypeSerializer;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.Map;

/**
 * A custom TypeSerializer that can serialize and deserialize a map of String keys and Object values. This is useful for maps where the user can specify multiple types of values, like a list of int and boolean values.
 *
 * @author darksaid98
 */
public final class StringObjectMapSerializer implements TypeSerializer<Map<String, Object>> {
    public static final StringObjectMapSerializer INSTANCE = new StringObjectMapSerializer();
    public static final TypeToken<Map<String, Object>> TYPE_TOKEN = new TypeToken<>() {
        @Override
        public Type getType() {
            return new ParameterizedType() {
                @Override
                public Type @NotNull [] getActualTypeArguments() {
                    return new Type[]{String.class, Object.class};
                }

                @Override
                public @NotNull Type getRawType() {
                    return Map.class;
                }

                @Override
                public Type getOwnerType() {
                    return null;
                }
            };
        }
    };

    @Override
    public Map<String, Object> deserialize(Type type, ConfigurationNode node) {
        final Map<String, Object> result = new HashMap<>();

        if (node.isMap()) {
            for (Map.Entry<Object, ? extends ConfigurationNode> entry : node.childrenMap().entrySet()) {
                final String key = entry.getKey().toString();
                final ConfigurationNode valueNode = entry.getValue();

                final Object value = valueNode.raw();

                if (valueNode.isMap()) {
                    result.put(key, deserialize(type, valueNode));
                } else {
                    result.put(key, value);
                }
            }
        }

        return result;
    }

    @Override
    @SuppressWarnings("unchecked")
    public void serialize(Type type, Map<String, Object> obj, ConfigurationNode node) throws SerializationException {
        if (obj == null)
            return;

        for (Map.Entry<String, Object> entry : obj.entrySet()) {
            final Object value = entry.getValue();

            if (value == null) {
                node.node(entry.getKey()).set(null);
                continue;
            }

            if (value instanceof Map) {
                serialize(type, (Map<String, Object>) value, node.node(entry.getKey()));
            } else {
                node.node(entry.getKey()).set(value);
            }
        }
    }
}
