package io.github.exampleuser.example.database.exception;

import java.io.Serial;

/**
 * Database migration exception is thrown whenever a Flyway migration fails.
 */
public class DatabaseMigrationException extends Exception {
    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * Instantiates a new Database migration exception.
     *
     * @param t the throwable
     */
    public DatabaseMigrationException(Throwable t) {
        super(t);
    }
}
