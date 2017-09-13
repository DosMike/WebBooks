package de.dosmike.sponge.WebBooks;

import java.util.Iterator;
import java.util.Optional;

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
import org.spongepowered.api.event.cause.Cause;
import org.spongepowered.api.item.inventory.Inventory;
import org.spongepowered.api.item.inventory.ItemStack;
import org.spongepowered.api.text.Text;

public class cmdUrl implements CommandExecutor {
	
	public static CommandSpec getCommandSpec() {
		 return CommandSpec.builder()
			.description(Text.of("Open a magic book from the mystical interwebz"))
			.extendedDescription(Text.of("Load a website with a max timeout of 3 seconds.\n -c will display the website in the chat instead of as a book\n -s will save the website in the target's inventory\n   Option c will not work with option s as this will supress all output"))
			.arguments(
					GenericArguments.flags().flag("c")
					.permissionFlag("webbooks.save", "s")
					.buildWith(GenericArguments.seq(
					GenericArguments.onlyOne(GenericArguments.string(Text.of("url"))),
					GenericArguments.optional(GenericArguments.player(Text.of("target")))
					)))
			.permission("webbooks.url.command")
			.executor(new cmdUrl())
			.build();
	}
	
	@Override
	public CommandResult execute(CommandSource src, CommandContext args) throws CommandException {

		Optional<String> url = args.getOne("url");
		
		Optional<Player> target = args.getOne("target");
		if (!target.isPresent()) {
			if (!(src instanceof Player)) { src.sendMessage(Text.of("Console can't do this")); return CommandResult.success(); }
			target = Optional.of((Player)src);
		} else if (!src.equals(target.get())) args.checkPermission(src, "webbooks.url.other");
		Player show = target.get();
		try {
			if (args.hasAny("s")) {
				WebBooks.loadUrl(url.get(), target.get(), website->{
					Sponge.getScheduler().createSyncExecutor(WebBooks.getInstance()).execute(()-> {
						ItemStack saved = website.save();
						Iterator<Inventory> playerInventory = show.getInventory().iterator();
						if (playerInventory.next().offer(saved).getRejectedItems().isEmpty()); //try to add the item into the hotbar, consume on success
						else if (!playerInventory.next().offer(saved).getRejectedItems().isEmpty()) { //if the second inventory (main inventory) can't hold the item, drop it
							Item item = (Item)show.getLocation().getExtent().createEntity(EntityTypes.ITEM, show.getLocation().getPosition().add(0.0, 1.62, 0.0));
							item.setCreator(show.getUniqueId());
							item.offer(Keys.REPRESENTED_ITEM, saved.createSnapshot());
							show.getLocation().getExtent().spawnEntity(item, Cause.builder().from(WebBooks.getInstance().BaseCause).owner(show).build());
						}
					});
				});
			} else if (args.hasAny("c"))
				WebBooks.loadUrl(url.get(), target.get(), website->{
					website.displayChat(show);
				});
			else
				WebBooks.loadUrl(url.get(), target.get(), website->{
					website.displayBook(show);
				});
		} catch (IllegalStateException exception) {
			src.sendMessage(Text.of("Hold on, already loading a website for ", show.getName() ,"..."));
		}
		return CommandResult.success();
	}
	
}
