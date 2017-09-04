package de.dosmike.sponge.WebBooks;

import org.spongepowered.api.Sponge;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.event.Listener;
import org.spongepowered.api.event.game.state.GameStartedServerEvent;
import org.spongepowered.api.plugin.Plugin;
import org.spongepowered.api.plugin.PluginContainer;
import org.spongepowered.api.plugin.PluginManager;

@Plugin(id="webbook", name="WebBooks", version="0.1", authors={"DosMike"})
public class WebBooks {
	private static WebBooks instance;
	static WebBooks getInstance() {
		return instance;
	}
	
	@Listener
	public void onServerStart(GameStartedServerEvent event) {
		instance = this;
		CommandRegistra.RegisterCommands();
		
		PluginContainer minecraft = Sponge.getPluginManager().getPlugin(PluginManager.MINECRAFT_PLUGIN_ID).get();
		PluginContainer sponge = Sponge.getPluginManager().getPlugin(PluginManager.SPONGE_PLUGIN_ID).get();
		PluginContainer container = Sponge.getPluginManager().fromInstance(this).get();
		
		Website.UserAgent = String.format("%s(%s/%s) %s/%s %s/%s(%s) %s",
				minecraft.getName(),
				Sponge.getPlatform().getExecutionType().toString(),
				Sponge.getPlatform().getType().toString(),
				minecraft.getVersion().orElse("?"),
				sponge.getName(),
				sponge.getVersion().orElse("?"),
				container.getName(),
				container.getId(),
				container.getVersion().orElse("?")
				);
	}
	
	public static void loadUrl(String url, Player player) {
		
	}
	
	public static void showHtml(String html, Player player) {
		
	}
}
