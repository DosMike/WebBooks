package de.dosmike.sponge.WebBooks;

import java.io.IOException;
import java.net.MalformedURLException;
import java.util.LinkedList;
import java.util.List;

import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.spongepowered.api.data.key.Keys;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.entity.living.player.gamemode.GameModes;
import org.spongepowered.api.text.BookView;
import org.spongepowered.api.text.Text;

public class Website {
	static String UserAgent="Minecraft-Server/WebBooks unknown";
	
	static Website fromUrl(String url, Player player) throws MalformedURLException, IOException {
		Connection con = Jsoup.connect(url).timeout(3000);
		con.header("User-Agent", UserAgent);
		//TODO read proxy from config
		con.data("Name", player.getName());
		con.data("UUID", player.getUniqueId().toString());
		con.data("World", player.getLocation().getExtent().getName() + "/" + player.getLocation().getExtent().getUniqueId().toString());
		con.data("Location", player.getLocation().getX()+"/"+player.getLocation().getY()+"/"+player.getLocation().getZ());
		con.data("Connection", player.getConnection().getAddress().getHostString()+":"+player.getConnection().getAddress().getPort()+"/"+ player.getConnection().getLatency()+"ms");
		con.data("Joined", player.getJoinData().lastPlayed().get().toEpochMilli()+"/"+player.getJoinData().firstPlayed().get().toEpochMilli());
		con.data("Status", player.get(Keys.HEALTH).orElse(-1.0)+"/"+player.get(Keys.FOOD_LEVEL).orElse(-1)+"/"+player.get(Keys.EXPERIENCE_LEVEL).orElse(-1)+"/"+player.get(Keys.GAME_MODE).orElse(GameModes.NOT_SET).toString());
		con.followRedirects(true);
		Website website = new Website();
		Document result = con.post();
		
		Element title = result.getElementsByTag("title").first();
		if (title != null) website.title = Text.of(title.text());
		else website.title=Text.of("Unnamed");
		
		result.getElementsByTag("ul").forEach(list->{
			if (list.classNames().contains("book")) {
				list.children().forEach(item->{
					website.pages.add(Text.of(item.text()));
				});
			}
		});
		
		return website;
	}
	static Website fromHtml(String html, Player player) {
		return null;
	}
	static void parseLink() {
		
	}

	
	List<Text> pages = new LinkedList<>();
	Text title;
	public Text getTitle() {
		return title;
	}
	public void display(Player player) {
		player.sendBookView(BookView.builder().title(title).addPages(pages).build());
	}
}
