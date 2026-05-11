package io.github.exampleuser.example.database.exception;

import java.io.Serial;

/**
 * Database initialization exception is thrown during database initialization.
 */
public class DatabaseInitializationException extends RuntimeException {
    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * Instantiates a new Database initialization exception.
     *
     * @param t the throwable
     */
    public DatabaseInitializationException(Throwable t) {
        super(t);
    }

    /**
     * Instantiates a new Database initialization exception.
     *
     * @param s the message
     * @param t the throwable
     */
    public DatabaseInitializationException(String s, Throwable t) {
        super(s, t);
    }

    /**
     * Instantiates a new Database initialization exception.
     *
     * @param s the message
     */
    public DatabaseInitializationException(String s) {
        super(s);
    }
}
