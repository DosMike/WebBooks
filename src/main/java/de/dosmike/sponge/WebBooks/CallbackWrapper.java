package de.dosmike.sponge.WebBooks;

import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.adventure.SpongeComponents;
import org.spongepowered.api.command.CommandCause;
import org.spongepowered.api.command.exception.CommandException;
import org.spongepowered.api.entity.living.player.server.ServerPlayer;
import org.spongepowered.api.service.permission.Subject;

import java.util.function.Consumer;

public class CallbackWrapper {

    @FunctionalInterface
    public interface SneakyCommandConsumer {
        void run(CommandCause cause) throws CommandException;
        static Consumer<CommandCause> silence(SneakyCommandConsumer c) {
            return cause -> {
                try {
                    c.run(cause);
                } catch (Throwable t) {
                    cause.audience().sendMessage(Component.text(t.getMessage(), NamedTextColor.RED));
                    t.printStackTrace();
                }
            };
        }
    }

    static ClickEvent playerPermissionCommandProxy(final String permission, final String command, final boolean atServer) {
        final String cmdExec;
        if (command == null || command.isEmpty()) cmdExec = "/help";
        else if (command.charAt(0) != '/') cmdExec = '/' + command;
        else cmdExec = command;

        if (permission == null)
            return SpongeComponents.executeCallback(SneakyCommandConsumer.silence(cc->{
                final ServerPlayer player = cc.root() instanceof ServerPlayer ? (ServerPlayer) cc.root() : null;
                if (player == null) WebBooks.w("Player not passed to callback as commandCause.root");
                final Subject subject = atServer ? Sponge.systemSubject() : player;
                final Audience audience = atServer ? Audience.audience(Sponge.server(), player) : player;
                    Sponge.server().commandManager().process(subject, audience, cmdExec);
            }));
        else
            return SpongeComponents.executeCallback(SneakyCommandConsumer.silence(cc->{
                final ServerPlayer player = cc.root() instanceof ServerPlayer ? (ServerPlayer) cc.root() : null;
                if (player == null) WebBooks.w("Player not passed to callback as commandCause.root");
                final Subject subject = atServer ? Sponge.systemSubject() : player;
                final Audience audience = atServer ? Audience.audience(Sponge.server(), player) : player;
                if (!player.hasPermission(permission)) {
                    player.sendMessage(Component.text().color(NamedTextColor.DARK_RED)
                            .content("You do not have the permission ")
                            .append(Component.text().content(permission).color(NamedTextColor.RED))
                            .build());
                } else {
                    Sponge.server().commandManager().process(subject, audience, cmdExec);
                }
            }));
    }

}
