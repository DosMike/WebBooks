package de.dosmike.sponge.WebBooks;

import com.google.inject.Inject;
import ninja.leaping.configurate.commented.CommentedConfigurationNode;
import ninja.leaping.configurate.loader.ConfigurationLoader;
import org.slf4j.Logger;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.config.DefaultConfig;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.event.Listener;
import org.spongepowered.api.event.game.GameReloadEvent;
import org.spongepowered.api.event.game.state.GameStartedServerEvent;
import org.spongepowered.api.event.network.ClientConnectionEvent;
import org.spongepowered.api.plugin.Plugin;
import org.spongepowered.api.plugin.PluginContainer;
import org.spongepowered.api.plugin.PluginManager;
import org.spongepowered.api.scheduler.SpongeExecutorService;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.text.format.TextColors;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.MalformedURLException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

@Plugin(id="webbook", name="WebBooks", version="1.2.1", authors={"DosMike"})
public class WebBooks {
	private static WebBooks instance;
	static WebBooks getInstance() {
		return instance;
	}

	static SpongeExecutorService executor;

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
		if (!Configuration.motd.isEmpty()) loadUrl(Configuration.motd, event.getTargetEntity(), website->{
			if (website.getResponseCode()==200) {
				website.displayBook(event.getTargetEntity());
			}
		});
	}
	
	@Listener
	public void onServerStart(GameStartedServerEvent event) {
		instance = this;
		executor = Sponge.getScheduler().createSyncExecutor(this);
		CommandRegistra.RegisterCommands();
		
		PluginContainer minecraft = Sponge.getPluginManager().getPlugin(PluginManager.MINECRAFT_PLUGIN_ID).get();
		PluginContainer sponge = Sponge.getPluginManager().getPlugin(PluginManager.SPONGE_PLUGIN_ID).get();
		PluginContainer container = Sponge.getPluginManager().fromInstance(this).get();

		Configuration.UserAgent = String.format("%s/%s %s/%s (%s; %s) %s/%s (%s; by DosMike)",
				minecraft.getName(),
				minecraft.getVersion().orElse("?"),
				sponge.getName(),
				sponge.getVersion().orElse("?"),
				Sponge.getPlatform().getExecutionType().name(),
				Sponge.getPlatform().getType().name(),
				container.getName(),
				container.getVersion().orElse("?"),
				container.getId()
				);
		
		if (!Sponge.getConfigManager().getSharedConfig(this).getConfigPath().toFile().exists()) {
			try {
				configManager.save(Configuration.generateDefault());
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		Configuration.reload();
	}
	
	@Listener
	public void onReload(GameReloadEvent event) {
		Configuration.reload();
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
				StringWriter sw = new StringWriter();
				PrintWriter pw = new PrintWriter(sw);
				e.printStackTrace(pw);
				pw.flush();
				pw.close();
				String[] lines = sw.toString().split("\n");
				if (lines.length > 5) {
					String andMore = "... and " + (lines.length - 5) + " more lines";
					lines = Arrays.copyOf(lines, 6);
					lines[5] = andMore;
				}
				String message = String.join("\n", lines);
				player.sendMessage(Text.of(TextColors.RED, "There was an error during execution:", Text.NEW_LINE, message));

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
	ConfigurationLoader<CommentedConfigurationNode> configManager;

}
