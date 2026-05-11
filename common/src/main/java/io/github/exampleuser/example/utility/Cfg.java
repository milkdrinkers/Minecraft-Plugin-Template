package io.github.exampleuser.example.utility;

import io.github.exampleuser.example.AbstractExample;
import io.github.exampleuser.example.config.ConfigHandler;
import io.github.exampleuser.example.config.PluginConfig;
import org.jetbrains.annotations.NotNull;

/**
 * Convenience class for accessing {@link ConfigHandler#getConfig}
 */
public final class Cfg {
    /**
     * Convenience method for {@link ConfigHandler#getConfig} to getConnection {@link PluginConfig}
     *
     * @return the config
     */
    @NotNull
    public static PluginConfig get() {
        return AbstractExample.getInstance().getConfigHandler().getConfig();
    }
}
