package io.github.exampleuser.exampleplugin.messaging.exception;

import java.io.Serial;

/**
 * Messaging initialization exception is thrown during message broker initialization.
 */
public class MessagingInitializationException extends RuntimeException {
    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * Instantiates a new Messaging initialization exception.
     *
     * @param t the throwable
     */
    public MessagingInitializationException(Throwable t) {
        super(t);
    }

    /**
     * Instantiates a new Messaging initialization exception.
     *
     * @param s the message
     * @param t the throwable
     */
    public MessagingInitializationException(String s, Throwable t) {
        super(s, t);
    }

    /**
     * Instantiates a new Messaging initialization exception.
     *
     * @param s the message
     */
    public MessagingInitializationException(String s) {
        super(s);
    }
}
