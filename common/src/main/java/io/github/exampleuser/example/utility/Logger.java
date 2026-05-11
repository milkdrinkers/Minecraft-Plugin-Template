package io.github.exampleuser.example.utility;


import io.github.exampleuser.example.AbstractExample;
import net.kyori.adventure.text.logger.slf4j.ComponentLogger;
import org.jetbrains.annotations.NotNull;

/**
 * A class that provides shorthand access to {@link AbstractExample#getComponentLogger}.
 */
public class Logger {
    /**
     * Get component logger. Shorthand for:
     *
     * @return the component logger {@link AbstractExample#getComponentLogger}.
     */
    @NotNull
    public static ComponentLogger get() {
        return AbstractExample.getInstance().getComponentLogger();
    }
}
