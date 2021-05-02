package de.dosmike.sponge.WebBooks;

import com.google.gson.stream.JsonWriter;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.inventory.Book;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.*;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.internal.StringUtil;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.Node;
import org.jsoup.nodes.TextNode;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.data.Keys;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.entity.living.player.server.ServerPlayer;
import org.spongepowered.api.item.ItemTypes;
import org.spongepowered.api.item.inventory.ItemStack;
import org.spongepowered.api.registry.RegistryTypes;
import org.spongepowered.api.service.pagination.PaginationList;

import java.io.IOException;
import java.io.StringWriter;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.*;
import java.util.function.Function;

public class Website {

	static Map<String, TextFormat> classNames = new HashMap<>();
	static {
		classNames.put("mc-0", NamedTextColor.BLACK);
		classNames.put("mc-1", NamedTextColor.DARK_BLUE);
		classNames.put("mc-2", NamedTextColor.DARK_GREEN);
		classNames.put("mc-3", NamedTextColor.DARK_AQUA);
		classNames.put("mc-4", NamedTextColor.DARK_RED);
		classNames.put("mc-5", NamedTextColor.DARK_PURPLE);
		classNames.put("mc-6", NamedTextColor.GOLD);
		classNames.put("mc-7", NamedTextColor.GRAY);
		classNames.put("mc-8", NamedTextColor.DARK_GRAY);
		classNames.put("mc-9", NamedTextColor.BLUE);
		classNames.put("mc-a", NamedTextColor.GREEN);
		classNames.put("mc-b", NamedTextColor.AQUA);
		classNames.put("mc-c", NamedTextColor.RED);
		classNames.put("mc-d", NamedTextColor.LIGHT_PURPLE);
		classNames.put("mc-e", NamedTextColor.YELLOW);
		classNames.put("mc-f", NamedTextColor.WHITE);
		classNames.put("mc-k", TextDecoration.OBFUSCATED);
		classNames.put("mc-l", TextDecoration.BOLD);
		classNames.put("mc-m", TextDecoration.STRIKETHROUGH);
		classNames.put("mc-n", TextDecoration.UNDERLINED);
		classNames.put("mc-o", TextDecoration.ITALIC);
	}

	private static String titleAttribUnescape(String title) {
		int index, idx2=0;
		StringBuilder sb = new StringBuilder(title.length());
		while ((index=title.indexOf('\\', idx2))>=0) {
			sb.append(title, idx2, index);
			index++; //look at the following char
			if (index >= title.length()) break; //unless we're oob
			if (title.charAt(index) == '\\') { // \\
				sb.append('\\');
			} else if (title.charAt(index) == 'n') { // \n
				sb.append("\n");
			} else { //invalid escapes, just print the character
				sb.append(title.charAt(index));
			}
			idx2 = index+1;
		}
		sb.append(title.substring(idx2));
		return sb.toString();
	}

	/**
	 * This is mirroring the implementation of StringUtil.normalizeString used in TextNode.text()
	 * with the difference that &amp;nbsp; is kept (using isWhitespace instead of isActualWhitespace).
	 * This is required for rendering as normal spaces and line breaks in HTML are expected to be
	 * normalized while &amp;nbsp; as non-breaking space has to remain!<br>
	 * NB: because U+00A0 does not print well in MC, it is replaced with regular spaces here.
	 * @param node the TextNode to extract coreValue from (getWholeText)
	 * @return the normalized text value more like in jsoup pre 1.13 (i think)
	 */
	private static String legacyText(TextNode node) {
		String text = node.getWholeText();
		StringBuilder sb = new StringBuilder(text.length());
		boolean lastWasWhite = false;

		int len = text.length();
		int c;
		for (int i = 0; i < len; i+= Character.charCount(c)) {
			c = text.codePointAt(i);
			if (StringUtil.isWhitespace(c)) {
				if (lastWasWhite)
					continue;
				sb.append(' ');
				lastWasWhite = true;
			} else if (c == '\u00A0') {
				sb.append(' ');
			} else if (!StringUtil.isInvisibleChar(c)) {
				sb.appendCodePoint(c);
				lastWasWhite = false;
			}
		}
		return sb.toString();
	}

	private static Component parseNodes(Node node) {
		if (node instanceof TextNode) {
			return Component.text(legacyText((TextNode) node));
		} else if (node instanceof Element) {
			Element elem = (Element)node;
			TextComponent.Builder builder = Component.text();
			List<Component> hoverText = new LinkedList<>();
			Style.Builder style = Style.style();

			switch (elem.tagName().toLowerCase()) {
			case "b":
			case "strong":
				style.decorate(TextDecoration.BOLD); break;
			case "u":
			case "mark":
				style.decorate(TextDecoration.UNDERLINED); break;
			case "i":
			case "em":
				style.decorate(TextDecoration.ITALIC); break;
			case "s":
			case "del":
				style.decorate(TextDecoration.STRIKETHROUGH); break;
			case "a":
				style.color(NamedTextColor.DARK_AQUA).decorate(TextDecoration.UNDERLINED);
				ClickActionHolder action = buildLinkAction(elem);
				builder.clickEvent(action.action());
				if (!elem.hasAttr("data-title-hide-href") && Configuration.extendedTooltips)
					hoverText.add(action.hoverText());
				break;
			default: break;
			}
			
			if (elem.hasAttr("class")) {
				//we could totally apply parsed css colors/decorations here - any good libs you know?
				elem.classNames().forEach(clzn->{
					if (classNames.containsKey(clzn)) {
						TextFormat format = classNames.get(clzn);
						if (format instanceof TextColor) { builder.color((TextColor) format); }
						else if (format instanceof TextDecoration) { builder.decorate((TextDecoration) format); }
						else throw new RuntimeException("Invalid format for web style-class "+clzn+": "+format.getClass().getName());
					}
				});
			}
			if (elem.hasAttr("title")) {
				Function<String,Component> tmapper;
				if (elem.hasAttr("data-title-escape")) {
					String tmp = elem.attr("data-title-escape");
					if (tmp.length()!=1) tmp = "&";
					tmapper = (s)->LegacyComponentSerializer.legacy(s.charAt(0)).deserialize(s);
				} else {
					tmapper = Component::text;
				}
				hoverText.add(0, tmapper.apply(titleAttribUnescape(elem.attr("title"))));
			}
			
			if (!hoverText.isEmpty()) {
				builder.hoverEvent(HoverEvent.showText(Component.join(Component.newline(), hoverText)));
			}
			
			node.childNodes().forEach(child->builder.append(parseNodes(child)));

			if (elem.isBlock() ||
				elem.tagName().equalsIgnoreCase("p") ||
				elem.tagName().equalsIgnoreCase("br"))
				builder.append(Component.newline());
			
			return builder.build();
		} else return Component.empty(); //unknown node type, probably comment
	}
	private static class ClickActionHolder {
		private final ClickEvent action;
		private final TextComponent text;
		public ClickActionHolder(ClickEvent action, TextComponent linkHover) {
			this.action=action;
			this.text=linkHover;
		}
		public ClickEvent action() { return action; }
		public TextComponent hoverText() { return text; }
	}
	private static ClickActionHolder buildLinkAction(Element elem) {
		String target = elem.attr("target");
		if (target.equalsIgnoreCase("_player")) {
			String cmd,cmdVis;
			if (elem.attr("href").isEmpty()) throw new IllegalArgumentException("href can't be empty for target=\"_player\"");
			if (elem.attr("href").charAt(0)=='/') {
				cmdVis = elem.attr("href");
				cmd = cmdVis.substring(1);
			} else {
				cmd = elem.attr("href");
				cmdVis = '/'+cmd;
			}

			String perm = elem.attr("data-permission");
			if (!perm.isEmpty())
				return new ClickActionHolder(CallbackWrapper.playerPermissionCommandProxy(perm, cmd, false), Component.text()
						.append(Component.text("¶ Run: ", NamedTextColor.GREEN))
						.append(Component.text(cmdVis)).build());
			else
				return new ClickActionHolder(ClickEvent.runCommand(cmdVis), Component.text()
						.append(Component.text("Run: ", NamedTextColor.GREEN))
						.append(Component.text(cmdVis)).build());
		} else if (target.equalsIgnoreCase("_server")) {
			String cmd,cmdVis;
			if (elem.attr("href").isEmpty()) throw new IllegalArgumentException("href can't be empty for target=\"_player\"");
			if (elem.attr("href").charAt(0)=='/') {
				cmdVis = elem.attr("href");
				cmd = cmdVis.substring(1);
			} else {
				cmd = elem.attr("href");
				cmdVis = '/'+cmd;
			}
			String perm = elem.attr("data-permission");
			if (!perm.isEmpty())
				return new ClickActionHolder(CallbackWrapper.playerPermissionCommandProxy(perm, cmd, true), Component.text()
						.append(Component.text("¶ Server: ", NamedTextColor.RED))
						.append(Component.text(cmdVis)).build());
			else
				return new ClickActionHolder(CallbackWrapper.playerPermissionCommandProxy(null, cmd, true), Component.text()
						.append(Component.text("Server: ", NamedTextColor.RED))
						.append(Component.text(cmdVis)).build());
		} else if (target.equalsIgnoreCase("_blank")) {
			try {
				return new ClickActionHolder(ClickEvent.openUrl(new URL(elem.absUrl("href"))), Component.text()
						.append(Component.text("Extern: ", NamedTextColor.AQUA))
						.append(Component.text(elem.attr("href"))).build());
			} catch (Exception e) {
				return new ClickActionHolder(null, Component.text("Broken Link", NamedTextColor.GRAY, TextDecoration.STRIKETHROUGH));
			}
		} else if (elem.attr("href").charAt(0)=='#') {
			String page = elem.attr("href").substring(1);
			try {
				int p = Integer.parseInt(page);
				return new ClickActionHolder(ClickEvent.changePage(p), Component.text("Goto page " + p, NamedTextColor.GRAY));
			} catch (Exception e) {
				return new ClickActionHolder(null, Component.text("Broken Page Ref", NamedTextColor.GRAY, TextDecoration.STRIKETHROUGH));
			}
		} else {
			return new ClickActionHolder(ClickEvent.runCommand("/webbook "+elem.absUrl("href")), Component.text()
					.append(Component.text("Url: ", NamedTextColor.GRAY))
					.append(Component.text(elem.attr("href"))).build());
		}
	}
	
	static Website fromUrl(URL url, ServerPlayer player) throws MalformedURLException, IOException {
		Connection con = Jsoup.connect(url.toString()).timeout(3000);
		con.header("User-Agent", Configuration.UserAgent);
		if (Configuration.proxy != null) con.proxy(Configuration.proxy);
		con.followRedirects(true);
		Locale playerLocale = player.locale();
		Locale serverLocale = Sponge.server().locale();
		con.header("Accept-Language",
				playerLocale.getLanguage()+"-"+playerLocale.getCountry()+", " +
				playerLocale.getLanguage()+";q=0.9, " +
				serverLocale.getLanguage()+";q=0.8, " +
				"*;q=0.5"
				);

		Document doc;
		if (Configuration.transportMethod.equalsIgnoreCase("get/header"))
			doc = sendPlayerInfo_Header(con, player);
		else if (Configuration.transportMethod.equalsIgnoreCase("post/formdata"))
			doc = sendPlayerInfo_PostFormData(con, player);
		else
			doc = sendPlayerInfo_PostJson(con, player);
		return parseDocument(doc, con.response().statusCode(), con.response().headers());
	}
	static Website fromHtml(String html, String baseUrl, Player player) {
		return parseDocument(Jsoup.parse(html, baseUrl), 200, new HashMap<>());
	}

	static Document sendPlayerInfo_PostFormData(Connection con, ServerPlayer player) throws IOException {
		con.header("Content-Type", "application/x-www-form-urlencoded");
		con.data("Name", player.name());
		con.data("UUID", player.uniqueId().toString());
		con.data("World", player.world().properties().key().formatted() + "/" + player.world().properties().uniqueId().toString());
		con.data("Location", player.position().x()+"/"+player.position().y()+"/"+player.position().z());
//		con.data("Connection", player.connection().address().getHostString()+":"+player.connection().address().getPort()+"/"+ player.connection().latency()+"ms");
		con.data("Joined", player.lastPlayed().get().toEpochMilli()+"/"+player.firstJoined().get().toEpochMilli());
		con.data("Status", player.get(Keys.HEALTH).orElse(-1.0)+"/"+player.get(Keys.FOOD_LEVEL).orElse(-1)+"/"+player.get(Keys.EXPERIENCE_LEVEL).orElse(-1)+"/"+player.gameMode().get().key(RegistryTypes.GAME_MODE).formatted());
		return con.post();
	}
	static Document sendPlayerInfo_Header(Connection con, ServerPlayer player) throws IOException {
		con.header("X-WebBook-User", player.name() + "; " + player.uniqueId().toString());
		con.header("X-WebBook-World", player.world().properties().key().formatted() + "; " + player.world().properties().uniqueId().toString());
		con.header("X-WebBook-Location", player.position().x()+"; "+player.position().y()+"; "+player.position().z());
//		con.header("X-WebBook-Connection", player.connection().address().getHostString()+":"+player.connection().address().getPort()+"; "+ player.connection().latency()+"ms");
		con.header("X-WebBook-Joined", player.lastPlayed().get().toEpochMilli()+"; "+player.firstJoined().get().toEpochMilli());
		con.header("X-WebBook-Status", player.get(Keys.HEALTH).orElse(-1.0)+"; "+player.get(Keys.FOOD_LEVEL).orElse(-1)+"; "+player.get(Keys.EXPERIENCE_LEVEL).orElse(-1)+"; "+player.gameMode().get().key(RegistryTypes.GAME_MODE).formatted());
		return con.get();
	}
	static Document sendPlayerInfo_PostJson(Connection con, ServerPlayer player) throws IOException {
		StringWriter sw = new StringWriter(1024);
		JsonWriter jw = new JsonWriter(sw);
		con.header("Content-Type", "application/json");
		jw.beginObject();
		jw.name("subject").beginObject();
		{
			jw.name("name").value(player.name());
			jw.name("uuid").value(player.uniqueId().toString());
			jw.name("health").value(player.get(Keys.HEALTH).orElse(Double.NaN));
			jw.name("foodLevel").value(player.get(Keys.FOOD_LEVEL).map(Double::valueOf).orElse(Double.NaN));
			jw.name("expLevel").value(player.get(Keys.EXPERIENCE_LEVEL).map(Double::valueOf).orElse(Double.NaN));
			jw.name("gameMode").value(player.gameMode().get().key(RegistryTypes.GAME_MODE).formatted());
		}
		jw.endObject();
		jw.name("location").beginObject();
		{
			jw.name("world").beginObject();
			jw.name("name").value(player.world().properties().key().formatted());
			jw.name("uuid").value(player.world().uniqueId().toString());
			jw.endObject();
			jw.name("position").beginObject();
			jw.name("x").value(player.position().x());
			jw.name("y").value(player.position().y());
			jw.name("z").value(player.position().z());
			jw.endObject();
		}
		jw.endObject();
		jw.name("connection").beginObject();
		{
//			jw.name("ip").value(player.connection().address().getHostString());
//			jw.name("port").value(player.connection().address().getPort());
//			jw.name("latency").value(player.connection().latency());
			jw.name("joined").beginObject();
			jw.name("first").value(player.firstJoined().get().toString());
			jw.name("last").value(player.lastPlayed().get().toString());
			jw.endObject();
		}
		jw.endObject();
		jw.endObject();
		jw.flush();
		jw.close();
		con.requestBody(sw.toString());
		return con.post();
	}

	private static Website parseDocument(Document doc, int rc, Map<String, String> headers) {
		Website website = new Website();
		website.url = doc.baseUri();
		website.recode = rc;
		website.reheaders = headers;
		Element title = doc.getElementsByTag("title").first();
		if (title != null) website.title = Component.text(title.text());
		else website.title=Component.text("Unnamed");

		doc.select(Configuration.pageSelector).forEach(item->
			website.pages.add(parseNodes(item))
		);
		
		return website;
	}

	String url;
	int recode;
	Map<String, String> reheaders;
	List<Component> pages = new LinkedList<>();
	Component title;
	
	/** Get the url this website was requested from
	 * @return the request url
	 */
	public String getUrl() {
		return url;
	}
	/** Get the response code from the web-server, 200 usually means everything's ok
	 * @return the response-code
	 */
	public int getResponseCode() {
		return recode;
	}
	/** Get all response headers, in case your web-application sends additional headers or idk
	 * @return a map containing the key-value mapping for the received headers
	 */
	public Map<String, String> getResponseHeaders() {
		return reheaders;
	}
	/** Gets the page title as returned in the &lt;head&gt;&lt;title&gt;-Tag
	 * @return the page title
	 */
	public Component getTitle() {
		return title;
	}
	/** All declared pages on this website, where pages are picked with the selector from the configuration.<br>
	 * The default value '<code><tt>ul.book li</tt></code>' would extract <code><tt>document.querySelectorAll("ul.book li")</tt></code>
	 * @return A collection of Text, each representing a page. */
	public Collection<Component> getPages() { return pages; }
	/** Show this website to a player wrapped in a bookview.
	 * @param player the player to show this website to 
	 */
	public void displayBook(ServerPlayer player) {
		player.openBook(Book.builder().title(title).pages(pages).build());
	}
	/** Show this website to a player presented by the chat paginator.
	 * @param receiver who ever is supposed to read the website
	 */
	public void displayChat(Audience receiver) {
		PaginationList.builder()
			.title(title)
			.contents(pages)
			.footer(Component.text(recode + " - " + url))
			.build()
			.sendTo(receiver);
	}

	/** Turns this website object into a signed book that can be opened over and over again, without
	 * any delay and no matter if the source website is available... how usefull :D
	 * @return An ItemStack containing 1 Book with all pages
	 */
	public ItemStack save() {
		return save(Configuration.defaultAuthor);
	}
	/** Turns this website object into a signed book that can be opened over and over again, without
	 * any delay and no matter if the source website is available... how usefull :D
	 * @return An ItemStack containing 1 Book with all pages
	 */
	public ItemStack save(Component author) {
		ItemStack stack = ItemStack.builder().itemType(ItemTypes.WRITTEN_BOOK).quantity(1).build();
		stack.offer(Keys.AUTHOR, author);
		stack.offer(Keys.PAGES, pages);
		stack.offer(Keys.DISPLAY_NAME, title);
		return stack;
	}
}
