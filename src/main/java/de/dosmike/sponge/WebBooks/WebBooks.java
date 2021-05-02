package de.dosmike.sponge.WebBooks;

import com.google.inject.Inject;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.apache.logging.log4j.Logger;
import org.spongepowered.api.Server;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.command.Command;
import org.spongepowered.api.config.DefaultConfig;
import org.spongepowered.api.entity.living.player.server.ServerPlayer;
import org.spongepowered.api.event.Listener;
import org.spongepowered.api.event.lifecycle.*;
import org.spongepowered.api.event.network.ServerSideConnectionEvent;
import org.spongepowered.api.plugin.PluginManager;
import org.spongepowered.api.scheduler.TaskExecutorService;
import org.spongepowered.configurate.CommentedConfigurationNode;
import org.spongepowered.configurate.loader.ConfigurationLoader;
import org.spongepowered.plugin.PluginContainer;
import org.spongepowered.plugin.jvm.Plugin;
import org.spongepowered.plugin.metadata.PluginMetadata;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

@Plugin("webbook")
public class WebBooks {
	private final PluginContainer plugin;
	private final Logger logger;

	private static WebBooks instance;

	static WebBooks getInstance() {
		return instance;
	}
	static PluginContainer container() {
		return instance.plugin;
	}

	@Inject
	public WebBooks(final PluginContainer plugin, final Logger logger) {
		if (WebBooks.instance != null) throw new IllegalStateException("Plugin Singleton already instantiated");
		WebBooks.instance = this;
		this.plugin = plugin;
		this.logger = logger;
	}
	static TaskExecutorService executor;
	static TaskExecutorService async;

	static void l(String format, Object... args) { instance.logger.info(String.format(format, args)); }
	static void w(String format, Object... args) { instance.logger.warn(String.format(format, args)); }
	
	static Set<UUID> loading = new HashSet<>();
	public boolean isWebsiteLoadingFor(ServerPlayer player) {
		return loading.contains(player.uniqueId());
	}

	@Listener
	public void onPlayerDisconnect(ServerSideConnectionEvent.Disconnect event) {
		loading.remove(event.player().uniqueId());
	}
	@Listener
	public void onPlayerJoin(ServerSideConnectionEvent.Join event) {
		if (!Configuration.motd.isEmpty()) loadUrl(Configuration.motd, event.player(), website->{
			if (website.getResponseCode()==200) {
				website.displayBook(event.player());
			}
		});
	}

	@Listener
	public void onConstructPlugin(final ConstructPluginEvent event) {
		PluginMetadata minecraft = Sponge.pluginManager().plugin(PluginManager.MINECRAFT_PLUGIN_ID).map(PluginContainer::getMetadata).get();
		PluginMetadata sponge = Sponge.pluginManager().plugin(PluginManager.SPONGE_PLUGIN_ID).map(PluginContainer::getMetadata).get();
		PluginMetadata container = Sponge.pluginManager().fromInstance(this).map(PluginContainer::getMetadata).get();

		Configuration.UserAgent = String.format("%s/%s %s/%s (%s; %s) %s/%s (%s; by DosMike)",
				minecraft.getName(),
				minecraft.getVersion(),
				sponge.getName(),
				sponge.getVersion(),
				Sponge.platform().executionType().name(),
				Sponge.platform().type().name(),
				container.getName(),
				container.getVersion(),
				container.getId()
		);
	}

	@Listener
	public void onServerStarting(final StartingEngineEvent<Server> event) {
		executor = Sponge.asyncScheduler().createExecutor(plugin);
		async = Sponge.asyncScheduler().createExecutor(plugin);

		if (!Sponge.configManager().sharedConfig(plugin).configPath().toFile().exists()) {
			try {
				configManager.save(Configuration.generateDefault());
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		Configuration.reload();
	}

	@Listener
	public void onRegisterCommands(final RegisterCommandEvent<Command.Parameterized> event) {
		event.register(container(), cmdUrl.getCommandSpec(), "webbook", "wbk", "url");
	}

	@Listener
	public void onServerStopping(final StoppingEngineEvent<Server> event) {
		async.shutdown();
		executor.shutdown();
	}

	@Listener
	public void onReload(RefreshGameEvent event) {
		Configuration.reload();
	}

	public static void loadUrl(String url, ServerPlayer player, WebsiteReadyConsumer callback) {
		try {
			loadUrl(new URL(url), player, callback);
		} catch (MalformedURLException u) {
			player.sendMessage(Component.text("Malformed URL: "+url));
		}
	}
	public static void loadUrl(URL url, ServerPlayer player, WebsiteReadyConsumer callback) {
		if (loading.contains(player.uniqueId())) throw new IllegalStateException("This player is already loading a website, try again in a moment");
		loading.add(player.uniqueId());
		async.execute(()->{
			try {
				callback.onWebsiteReady(Website.fromUrl(url, player));
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
				player.sendMessage(Component.text("There was an error during execution:" + Component.newline() + message, NamedTextColor.RED));

				e.printStackTrace();
			} finally {
				loading.remove(player.uniqueId());
			}
		});
	}
	
	public static Website parseHtml(String html, String baseURL, ServerPlayer player) {
		return Website.fromHtml(html, baseURL, player);
	}
	
	@Inject
	@DefaultConfig(sharedRoot = true)
	ConfigurationLoader<CommentedConfigurationNode> configManager;

}
