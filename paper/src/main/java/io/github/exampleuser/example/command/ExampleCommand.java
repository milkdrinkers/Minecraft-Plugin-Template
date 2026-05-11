package io.github.exampleuser.example.command;

import dev.jorel.commandapi.CommandAPICommand;
import dev.jorel.commandapi.executors.CommandArguments;
import io.github.exampleuser.example.AbstractExample;
import io.github.milkdrinkers.colorparser.paper.ColorParser;
import org.bukkit.command.CommandSender;

import static io.github.exampleuser.example.command.CommandHandler.BASE_PERM;

/**
 * Class containing the code for the example command.
 */
final class ExampleCommand extends Command {
    private final AbstractExample plugin;

    /**
     * Instantiates and registers a new command.
     */
    ExampleCommand(AbstractExample plugin) {
        this.plugin = plugin;
    }

    @Override
    public CommandAPICommand command() {
        return new CommandAPICommand("example")
            .withHelp("Base command.", "Base command.")
            .withPermission(BASE_PERM)
            .withSubcommands(
                new TranslationCommand().command(),
                new DumpCommand().command()
            )
            .executes(this::executorExample);
    }

    private void executorExample(CommandSender sender, CommandArguments args) {
        sender.sendMessage(
            ColorParser.of("<white>Read more about CommandAPI &9<click:open_url:'https://commandapi.jorel.dev/9.0.3/'>here</click><white>.")
                .legacy() // Parse legacy color codes
                .build()
        );
    }
}
