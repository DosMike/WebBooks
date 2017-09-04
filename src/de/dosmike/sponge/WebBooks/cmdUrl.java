package de.dosmike.sponge.WebBooks;

import java.util.Optional;

import org.spongepowered.api.Sponge;
import org.spongepowered.api.command.CommandException;
import org.spongepowered.api.command.CommandResult;
import org.spongepowered.api.command.CommandSource;
import org.spongepowered.api.command.args.CommandContext;
import org.spongepowered.api.command.args.GenericArguments;
import org.spongepowered.api.command.spec.CommandExecutor;
import org.spongepowered.api.command.spec.CommandSpec;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.text.Text;

public class cmdUrl implements CommandExecutor {
	
	public static CommandSpec getCommandSpec() {
		 return CommandSpec.builder()
			.description(Text.of("Open a magic book from the mystical interwebz"))
			.arguments(
					GenericArguments.onlyOne(GenericArguments.string(Text.of("url"))),
					GenericArguments.optional(GenericArguments.player(Text.of("target")))
					)
//			.permission("webbooks.cmd.url")
			.executor(new cmdUrl())
			.build();
	}
	
	@Override
	public CommandResult execute(CommandSource src, CommandContext args) throws CommandException {

		Optional<Player> target = args.getOne("target");
		Optional<String> url = args.getOne("url");
		
		if (!target.isPresent()) {
			if (!(src instanceof Player)) { src.sendMessage(Text.of("Console can't do this")); return CommandResult.success(); }
			target = Optional.of((Player)src);
		}
		Player callon = target.get();
		
		Sponge.getScheduler().createAsyncExecutor(WebBooks.getInstance()).execute(()->{
			try {
				Website.fromUrl(url.get(), callon).display(callon);
			} catch (Exception e) {
				e.printStackTrace();
			}
		});
		
		return CommandResult.success();
	}
	
}
