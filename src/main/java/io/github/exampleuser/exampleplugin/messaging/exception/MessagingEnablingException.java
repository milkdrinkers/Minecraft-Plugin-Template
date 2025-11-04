package io.github.exampleuser.exampleplugin.messaging.exception;

import java.io.Serial;

/**
 * Messaging enabling exception is thrown during message broker enabling.
 */
public class MessagingEnablingException extends RuntimeException {
    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * Instantiates a new Messaging enabling exception.
     *
     * @param t the throwable
     */
    public MessagingEnablingException(Throwable t) {
        super(t);
    }

    /**
     * Instantiates a new Messaging enabling exception.
     *
     * @param s the message
     * @param t the throwable
     */
    public MessagingEnablingException(String s, Throwable t) {
        super(s, t);
    }

    /**
     * Instantiates a new Messaging enabling exception.
     *
     * @param s the message
     */
    public MessagingEnablingException(String s) {
        super(s);
    }
}
