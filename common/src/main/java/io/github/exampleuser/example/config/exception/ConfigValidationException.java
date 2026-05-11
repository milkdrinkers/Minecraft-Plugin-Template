package io.github.exampleuser.example.config.exception;

import org.spongepowered.configurate.serialize.SerializationException;

import java.io.Serial;

public class ConfigValidationException extends SerializationException {
    private static @Serial final long serialVersionUID = 1L;

    public ConfigValidationException(String message) {
        super(message);
    }
}
