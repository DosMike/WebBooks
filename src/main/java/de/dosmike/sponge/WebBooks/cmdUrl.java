package de.dosmike.sponge.WebBooks;

import org.jetbrains.annotations.NotNull;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.command.CommandException;
import org.spongepowered.api.command.CommandResult;
import org.spongepowered.api.command.CommandSource;
import org.spongepowered.api.command.args.CommandContext;
import org.spongepowered.api.command.args.GenericArguments;
import org.spongepowered.api.command.spec.CommandExecutor;
import org.spongepowered.api.command.spec.CommandSpec;
import org.spongepowered.api.data.key.Keys;
import org.spongepowered.api.entity.EntityTypes;
import org.spongepowered.api.entity.Item;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.item.inventory.Inventory;
import org.spongepowered.api.item.inventory.ItemStack;
import org.spongepowered.api.service.permission.Subject;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.text.format.TextColors;
import org.spongepowered.api.text.serializer.TextSerializers;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Iterator;
import java.util.Locale;
import java.util.UUID;
import java.util.function.Supplier;
import java.util.regex.Pattern;

public class cmdUrl implements CommandExecutor {
	
	public static CommandSpec getCommandSpec() {
		 return CommandSpec.builder()
			.description(Text.of("Open a magic book from the mystical interwebz"))
			.extendedDescription(Text.of("Load a website with a max timeout of 3 seconds.\n -c will display the website in the chat instead of as a book\n -s will save the website in the target's inventory\n   Option c will not work with option s as this will supress all output"))
			.arguments(
					GenericArguments.flags().flag("c")
					.permissionFlag(Permissions.SAVE, "s")
					.valueFlag(GenericArguments.requiringPermission(GenericArguments.string(Text.of("author")), Permissions.AUTHOR), "a")
					.buildWith(GenericArguments.seq(
					GenericArguments.onlyOne(GenericArguments.string(Text.of("url"))),
					GenericArguments.optional(GenericArguments.player(Text.of("target")))
					)))
			.permission(Permissions.COMMAND)
			.executor(new cmdUrl())
			.build();
	}

	private void printBook(String url, Player target, Text author) {
		UUID uuid = target.getUniqueId();
		WebBooks.loadUrl(url, target, website->
			WebBooks.executor.execute(()->
				Sponge.getServer().getPlayer(uuid).ifPresent(show-> {
					ItemStack saved = website.save(author);
					Iterator<Inventory> playerInventory = show.getInventory().iterator();
					if (playerInventory.next().offer(saved).getRejectedItems().isEmpty()); //try to add the item into the hotbar, consume on success
					else if (!playerInventory.next().offer(saved).getRejectedItems().isEmpty()) { //if the second inventory (main inventory) can't hold the item, drop it
						Item item = (Item)show.getLocation().getExtent().createEntity(EntityTypes.ITEM, show.getLocation().getPosition().add(0.0, 1.62, 0.0));
						item.setCreator(show.getUniqueId());
						item.offer(Keys.REPRESENTED_ITEM, saved.createSnapshot());
						show.getLocation().getExtent().spawnEntity(item);
					}
				})
			)
		);
	}

	private static final Pattern ippat = Pattern.compile("(\\d+\\.\\d+\\.\\d+\\.\\d+)|(\\[([0-9a-fA-F]{2})?(:([0-9a-fA-F]{2})?)+\\])");
	boolean checkDomain(String url, Subject subject) {
		try {
			String host = new URI(url).getHost();
			if (ippat.matcher(host).matches()) return false; //ips are not allowed
			//reverse domain
			String[] parts = host.split("\\.");
			StringBuilder perm = new StringBuilder(Permissions.DOMAIN_BASE);
			for (int i = parts.length-1; i>=0; i--) perm.append(".").append(parts[i].toLowerCase(Locale.ROOT));
			return subject.hasPermission(perm.toString());
		} catch (URISyntaxException e) {
			return false;
		}
	}

	private Supplier<Player> playerRef(Player player) {
		UUID uuid = player.getUniqueId();
		return ()->Sponge.getServer().getPlayer(uuid).orElseThrow(()->new RuntimeException("Player "+ uuid +" left the server"));
	}

	@Override @NotNull
	public CommandResult execute(@NotNull CommandSource src, CommandContext args) throws CommandException {

		String url = args.<String>getOne("url").get();
		
		Player target = args.<Player>getOne("target").orElseGet(()->src instanceof Player ? (Player)src : null);
		if (target == null) {
			throw new CommandException(Text.of(TextColors.RED, "Console can't do this"));
		}
		if (!src.equals(target)) args.checkPermission(src, Permissions.TARGET);

		if (!checkDomain(url, src)) {
			throw new CommandException(Text.of(TextColors.RED, "The specified URI was invalid or blocked"));
		}
		Supplier<Player> show = playerRef(target);

		try {
			Text author = args.<String>getOne(Text.of("author"))
					.map(TextSerializers.FORMATTING_CODE::deserialize)
					.orElse(Configuration.defaultAuthor);
			if (args.hasAny("s")) {
				printBook(url, target, author);
			} else if (args.hasAny("c"))
				WebBooks.loadUrl(url, target, website -> website.displayChat(show.get()));
			else
				WebBooks.loadUrl(url, target, website -> website.displayBook(show.get()));
		} catch (IllegalStateException exception) {
			src.sendMessage(Text.of("Hold on, already loading a website for ", target.getName() ,"..."));
		}
		return CommandResult.success();
	}
	
}
