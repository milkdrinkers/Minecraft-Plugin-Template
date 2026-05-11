package io.github.exampleuser.example.command;

import dev.jorel.commandapi.CommandAPICommand;
import dev.jorel.commandapi.CommandAPIPaper;
import dev.jorel.commandapi.exceptions.WrapperCommandSyntaxException;
import io.github.milkdrinkers.colorparser.paper.ColorParser;
import io.github.milkdrinkers.wordweaver.Translation;
import net.kyori.adventure.text.ComponentLike;

@SuppressWarnings("unused")
public abstract class Command {
    public abstract CommandAPICommand command();

    public sealed interface Result {
        static Result ok() {
            return Ok.INSTANCE;
        }

        static CommandException fail(ComponentLike message) {
            return new CommandException(message);
        }

        static CommandException fail(String translationKey) {
            return new CommandException(ColorParser.of(Translation.of(translationKey)).build());
        }

        static Result exception(ComponentLike message) throws CommandException {
            throw fail(message);
        }

        static Result exception(String translationKey) throws CommandException {
            throw fail(translationKey);
        }

        record Ok() implements Result {
            static final Ok INSTANCE = new Ok();
        }

        record Fail(ComponentLike message) implements Result {
        }

        class CommandException extends WrapperCommandSyntaxException {
            private final ComponentLike message;

            public CommandException(ComponentLike message) {
                super(CommandAPIPaper.failWithAdventureComponent(message).getException());
                this.message = message;
            }

            public ComponentLike getClientMessage() {
                return message;
            }
        }
    }
}
