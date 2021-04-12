package de.dosmike.sponge.WebBooks;

import org.spongepowered.api.Sponge;

public class CommandRegistra {
	public static void RegisterCommands() {
		Sponge.getCommandManager().register(WebBooks.getInstance(), cmdUrl.getCommandSpec(), "webbook", "wbk", "url");
	}
}
