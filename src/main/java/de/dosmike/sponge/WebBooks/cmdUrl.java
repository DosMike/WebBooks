package de.dosmike.sponge.WebBooks;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.command.Command;
import org.spongepowered.api.command.CommandExecutor;
import org.spongepowered.api.command.CommandResult;
import org.spongepowered.api.command.exception.CommandException;
import org.spongepowered.api.command.parameter.CommandContext;
import org.spongepowered.api.command.parameter.Parameter;
import org.spongepowered.api.command.parameter.managed.Flag;
import org.spongepowered.api.data.Keys;
import org.spongepowered.api.entity.EntityTypes;
import org.spongepowered.api.entity.Item;
import org.spongepowered.api.entity.living.player.server.ServerPlayer;
import org.spongepowered.api.item.inventory.ItemStack;
import org.spongepowered.api.item.inventory.transaction.InventoryTransactionResult;
import org.spongepowered.api.service.permission.Subject;

import java.net.URL;
import java.util.Locale;
import java.util.UUID;
import java.util.function.Supplier;
import java.util.regex.Pattern;

public class cmdUrl implements CommandExecutor {

	private static Parameter.Value<Component> FLAG_AUTHOR_PARAM_AUTHOR;
	private static Parameter.Value<URL> PARAM_URL;
	private static Parameter.Value<ServerPlayer> PARAM_TARGET;
	private static Flag FLAG_CONSOLE;
	private static Flag FLAG_SAVE;
	private static Flag FLAG_AUTHOR;
	private static boolean init = false;

	public static Command.Parameterized getCommandSpec() {
		if (!init) {
			init=true;
			FLAG_AUTHOR_PARAM_AUTHOR = Parameter.formattingCodeText()
					.key("author")
					.build();
			PARAM_URL = Parameter.url()
					.key("url")
					.terminal().build();
			PARAM_TARGET = Parameter.player()
					.key("target")
					.requiredPermission(Permissions.TARGET)
					.optional().build();
			FLAG_CONSOLE = Flag.builder()
					.alias("c")
					.build();
			FLAG_SAVE = Flag.builder()
					.alias("s")
					.setPermission(Permissions.SAVE)
					.build();
			FLAG_AUTHOR = Flag.builder()
					.alias("a")
					.setPermission(Permissions.AUTHOR)
					.setParameter(FLAG_AUTHOR_PARAM_AUTHOR)
					.build();
		}

		return Command.builder()
				.shortDescription(Component.text("Open a magic book from the mystical interwebz"))
				.extendedDescription(Component.text("Load a website with a max timeout of 3 seconds.\n -c will display the website in the chat instead of as a book\n -s will save the website in the target's inventory\n   Option c will not work with option s as this will supress all output"))
				.addFlag(FLAG_CONSOLE)
				.addFlag(FLAG_SAVE)
				.addFlag(FLAG_AUTHOR)
				.addParameter(PARAM_URL)
				.addParameter(PARAM_TARGET)
				.permission(Permissions.COMMAND)
				.executor(new cmdUrl())
				.build();
	}

	private void printBook(URL url, ServerPlayer target, Component author) {
		UUID uuid = target.uniqueId();
		WebBooks.loadUrl(url, target, website->
			WebBooks.executor.execute(()->
				Sponge.server().player(uuid).ifPresent(show-> {
					ItemStack saved = website.save(author);
					InventoryTransactionResult result = show.inventory().offer(saved);
					if (!result.rejectedItems().isEmpty()) {
						Item item = show.location().world().createEntity(EntityTypes.ITEM, show.location().position().add(0.0, 1.62, 0.0));
						item.offer(Keys.CREATOR, uuid);
						item.offer(Keys.ITEM_STACK_SNAPSHOT, saved.createSnapshot());
						show.location().world().spawnEntity(item);
					}
				})
			)
		);
	}

	private static final Pattern ippat = Pattern.compile("(\\d+\\.\\d+\\.\\d+\\.\\d+)|(\\[([0-9a-fA-F]{2})?(:([0-9a-fA-F]{2})?)+\\])");
	boolean checkDomain(URL url, Subject subject) {
		String host = url.getHost();
		if (ippat.matcher(host).matches()) return false; //ips are not allowed
		//reverse domain
		String[] parts = host.split("\\.");
		StringBuilder perm = new StringBuilder(Permissions.DOMAIN_BASE);
		for (int i = parts.length-1; i>=0; i--) perm.append(".").append(parts[i].toLowerCase(Locale.ROOT));
		return subject.hasPermission(perm.toString());
	}

	private Supplier<ServerPlayer> playerRef(ServerPlayer player) {
		UUID uuid = player.uniqueId();
		return ()->Sponge.server().player(uuid).orElseThrow(()->new RuntimeException("Player "+ uuid +" left the server"));
	}

	@Override
	public CommandResult execute(CommandContext args) throws CommandException {

		URL url = args.requireOne(PARAM_URL);
		
		ServerPlayer target = args.one(PARAM_TARGET).orElseGet(()->
				args.cause().root() instanceof ServerPlayer
				? (ServerPlayer)args.cause().root()
				: null);
		if (target == null) {
			throw new CommandException(Component.text("Console can't do this", NamedTextColor.RED));
		}

		if (!checkDomain(url, target)) {
			throw new CommandException(Component.text("The specified URI was invalid or blocked", NamedTextColor.RED));
		}
		Supplier<ServerPlayer> show = playerRef(target);

		try {
			Component author = args.one(FLAG_AUTHOR_PARAM_AUTHOR)
					.orElse(Configuration.defaultAuthor);
			if (args.hasFlag(FLAG_SAVE)) {
				printBook(url, target, author);
			} else if (args.hasFlag(FLAG_CONSOLE))
				WebBooks.loadUrl(url, target, website -> website.displayChat(show.get()));
			else
				WebBooks.loadUrl(url, target, website -> website.displayBook(show.get()));
		} catch (IllegalStateException exception) {
			args.sendMessage(target, Component.text()
					.content("Hold on, already loading a website for ")
					.append(target.displayName().get())
					.append(Component.text("..."))
					.build());
		}
		return CommandResult.success();
	}
	
}
