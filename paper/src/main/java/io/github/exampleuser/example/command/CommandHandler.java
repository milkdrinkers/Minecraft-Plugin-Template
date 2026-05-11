package io.github.exampleuser.example.command;

import dev.jorel.commandapi.CommandAPI;
import dev.jorel.commandapi.CommandAPIPaperConfig;
import io.github.exampleuser.example.AbstractExample;
import io.github.exampleuser.example.Example;
import io.github.exampleuser.example.Reloadable;

/**
 * A class to handle registration of commands.
 */
public class CommandHandler implements Reloadable {
    public static final String BASE_PERM = "example.command";
    private final Example plugin;

    /**
     * Instantiates the Command handler.
     *
     * @param plugin the plugin
     */
    public CommandHandler(Example plugin) {
        this.plugin = plugin;
    }

    @Override
    public void onLoad(AbstractExample plugin) {
        CommandAPI.onLoad(
            new CommandAPIPaperConfig(plugin)
                .silentLogs(true)
        );
    }

    @Override
    public void onEnable(AbstractExample plugin) {
        if (!CommandAPI.isLoaded())
            return;

        CommandAPI.onEnable();

        // Register commands here
        new ExampleCommand(plugin)
            .command()
            .withAliases()
            .register();
    }

    @Override
    public void onDisable(AbstractExample plugin) {
        if (!CommandAPI.isLoaded())
            return;

        CommandAPI.onDisable();
    }
}