package de.dosmike.sponge.WebBooks;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.MalformedURLException;
import java.net.Proxy;
import java.net.Proxy.Type;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import org.slf4j.Logger;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.config.DefaultConfig;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.event.Listener;
import org.spongepowered.api.event.cause.Cause;
import org.spongepowered.api.event.cause.NamedCause;
import org.spongepowered.api.event.game.GameReloadEvent;
import org.spongepowered.api.event.game.state.GameStartedServerEvent;
import org.spongepowered.api.event.network.ClientConnectionEvent;
import org.spongepowered.api.plugin.Plugin;
import org.spongepowered.api.plugin.PluginContainer;
import org.spongepowered.api.plugin.PluginManager;
import org.spongepowered.api.text.Text;

import com.google.inject.Inject;

import ninja.leaping.configurate.ConfigurationNode;
import ninja.leaping.configurate.commented.CommentedConfigurationNode;
import ninja.leaping.configurate.loader.ConfigurationLoader;

@Plugin(id="webbook", name="WebBooks", version="1.1", authors={"DosMike"})
public class WebBooks {
	private static WebBooks instance;
	static WebBooks getInstance() {
		return instance;
	}
	Cause BaseCause;
	static String motd=null;
	
	@Inject
	private Logger logger;
	static void l(String format, Object... args) { instance.logger.info(String.format(format, args)); }
	static void w(String format, Object... args) { instance.logger.warn(String.format(format, args)); }
	
	static Set<UUID> loading = new HashSet<>();
	public boolean isWebsiteLoadingFor(Player player) {
		return loading.contains(player.getUniqueId());
	}
	@Listener
	public void onPlayerDisconnect(ClientConnectionEvent.Disconnect event) {
		loading.remove(event.getTargetEntity().getUniqueId());
	}
	@Listener
	public void onPlayerJoin(ClientConnectionEvent.Join event) {
		if (!motd.isEmpty()) loadUrl(motd, event.getTargetEntity(), website->{
			if (website.getResponseCode()==200) {
				website.displayBook(event.getTargetEntity());
			}
		});
	}
	
	@Listener
	public void onServerStart(GameStartedServerEvent event) {
		instance = this;
		CommandRegistra.RegisterCommands();
		
		PluginContainer minecraft = Sponge.getPluginManager().getPlugin(PluginManager.MINECRAFT_PLUGIN_ID).get();
		PluginContainer sponge = Sponge.getPluginManager().getPlugin(PluginManager.SPONGE_PLUGIN_ID).get();
		PluginContainer container = Sponge.getPluginManager().fromInstance(this).get();
		BaseCause = Cause.of(NamedCause.of("WebBooks", container));
		
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
		
		if (!Sponge.getConfigManager().getSharedConfig(this).getConfigPath().toFile().exists()) {
			CommentedConfigurationNode root = configManager.createEmptyNode();
			root.getNode("Proxy").setComment("Here you can set a HTTP-Proxy for the Game-Server to use when connecting to the Web-Server. Note: The overall delay must not exceed 3 seconds!");
			root.getNode("Proxy", "Host").setValue("");
			root.getNode("Proxy", "Host").setComment("Leave this value empty to disable the proxy");
			root.getNode("Proxy", "Port").setValue(8080);
			root.getNode("MOTD").setComment("Specify a URL here that will be displayed to every player that joins the server");
			try {
				configManager.save(root);
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		loadConfig();
	}
	
	@Listener
	public void onReload(GameReloadEvent event) {
		loadConfig();
	}
	
	public static void loadUrl(String url, Player player, WebsiteReadyConsumer callback) {
		if (loading.contains(player.getUniqueId())) throw new IllegalStateException("This player is already loading a website, try again in a moment");
		loading.add(player.getUniqueId());
		Sponge.getScheduler().createAsyncExecutor(WebBooks.getInstance()).execute(()->{
			try {
				callback.onWebsiteReady(Website.fromUrl(url, player));
			} catch (MalformedURLException u) {
				player.sendMessage(Text.of("Malformed URL: "+url));
			} catch (Exception e) {
				e.printStackTrace();
			} finally {
				loading.remove(player.getUniqueId());
			}
		});
	}
	
	public static Website parseHtml(String html, String baseURL, Player player) {
		return Website.fromHtml(html, baseURL, player);
	}
	
	@Inject
	@DefaultConfig(sharedRoot = true)
	private ConfigurationLoader<CommentedConfigurationNode> configManager;
	private void loadConfig() {
		try {
			ConfigurationNode s = configManager.load();
			
			String host = s.getNode("Proxy", "Host").getString("");
			int port = s.getNode("Proxy", "Port").getInt(8080);
			if (host.isEmpty()) Website.proxy = null;
			else Website.proxy = new Proxy(Type.HTTP, new InetSocketAddress(host, port));
			
			motd = s.getNode("MOTD").getString("");
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
