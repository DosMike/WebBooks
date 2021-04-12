package de.dosmike.sponge.WebBooks;

import com.google.gson.stream.JsonWriter;
import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.internal.StringUtil;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.Node;
import org.jsoup.nodes.TextNode;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.data.key.Keys;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.entity.living.player.gamemode.GameModes;
import org.spongepowered.api.item.ItemTypes;
import org.spongepowered.api.item.inventory.ItemStack;
import org.spongepowered.api.service.pagination.PaginationList;
import org.spongepowered.api.text.BookView;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.text.action.ClickAction;
import org.spongepowered.api.text.action.TextActions;
import org.spongepowered.api.text.channel.MessageReceiver;
import org.spongepowered.api.text.format.*;

import java.io.IOException;
import java.io.StringWriter;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.*;

public class Website {

	static Map<String, Object> classNames = new HashMap<>();
	static {
		classNames.put("mc-0", TextColors.BLACK);
		classNames.put("mc-1", TextColors.DARK_BLUE);
		classNames.put("mc-2", TextColors.DARK_GREEN);
		classNames.put("mc-3", TextColors.DARK_AQUA);
		classNames.put("mc-4", TextColors.DARK_RED);
		classNames.put("mc-5", TextColors.DARK_PURPLE);
		classNames.put("mc-6", TextColors.GOLD);
		classNames.put("mc-7", TextColors.GRAY);
		classNames.put("mc-8", TextColors.DARK_GRAY);
		classNames.put("mc-9", TextColors.BLUE);
		classNames.put("mc-a", TextColors.GREEN);
		classNames.put("mc-b", TextColors.AQUA);
		classNames.put("mc-c", TextColors.RED);
		classNames.put("mc-d", TextColors.LIGHT_PURPLE);
		classNames.put("mc-e", TextColors.YELLOW);
		classNames.put("mc-f", TextColors.WHITE);
		classNames.put("mc-k", TextStyles.OBFUSCATED);
		classNames.put("mc-l", TextStyles.BOLD);
		classNames.put("mc-m", TextStyles.STRIKETHROUGH);
		classNames.put("mc-n", TextStyles.UNDERLINE);
		classNames.put("mc-o", TextStyles.ITALIC);
		classNames.put("mc-r", TextFormat.of(TextColors.RESET, TextStyles.RESET));
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

	private static Text parseNodes(Node node) {
		if (node instanceof TextNode) {
			return Text.of(legacyText((TextNode) node));
		} else if (node instanceof Element) {
			Element elem = (Element)node;
			Text.Builder builder = Text.builder();
			List<Text> hoverText = new LinkedList<>();
			switch (elem.tagName().toLowerCase()) {
			case "b": 
				builder.format(builder.getFormat().style(TextStyles.BOLD) ); break;
			case "u": 
				builder.format(builder.getFormat().style(TextStyles.UNDERLINE) ); break;
			case "i": 
				builder.format(builder.getFormat().style(TextStyles.ITALIC) ); break;
			case "s": 
				builder.format(builder.getFormat().style(TextStyles.STRIKETHROUGH) ); break;
			case "a":
				builder.format(builder.getFormat().style(TextStyles.UNDERLINE).color(TextColors.DARK_AQUA));
				ClickActionHolder action = buildLinkAction(elem);
				builder.onClick(action.getAction());
				if (!elem.hasAttr("data-title-hide-href") && Configuration.extendedTooltips)
					hoverText.add(action.getHoverText());
				break;
			default: break;
			}
			
			if (elem.hasAttr("class")) {
				elem.classNames().forEach(clzn->{
					if (classNames.containsKey(clzn)) {
						Object format = classNames.get(clzn);
						if (format instanceof TextStyle) { builder.format(builder.getFormat().style((TextStyle)format)); }
						else if (format instanceof TextColor) { builder.format(builder.getFormat().color((TextColor)format)); }
						else if (format instanceof TextFormat) { builder.format((TextFormat)format); }
						else throw new RuntimeException("Invalid format for web style-class "+clzn+": "+format.getClass().getName());
					}
				});
			}
			if (elem.hasAttr("title")) {
				hoverText.add(0, Text.of(titleAttribUnescape(elem.attr("title"))));
			}
			
			if (!hoverText.isEmpty()) {
				Text.Builder smashed = Text.builder();
				boolean first=true; for (Text line : hoverText) { if (first) first=false; else smashed.append(Text.NEW_LINE); smashed.append(line); }
				builder.onHover(TextActions.showText(smashed.build()));
			}
			
			node.childNodes().forEach(child->builder.append(parseNodes(child)));
//			builder.append(Text.of(TextColors.NONE, TextColors.RESET, TextStyles.NONE, TextStyles.RESET));// encapsulate the stile within this text - looks strange, but works ;D
			builder.append(Text.of("§r"));
			
			if (elem.isBlock()) builder.append(Text.NEW_LINE);
			if (elem.tagName().equalsIgnoreCase("p")||elem.tagName().equalsIgnoreCase("br")) builder.append(Text.NEW_LINE);
			
			return builder.build();
		} else return Text.EMPTY; //unknown node type, probably comment
	}
	private static class ClickActionHolder {
		private final ClickAction<?> action;
		private final Text text;
		public ClickActionHolder(ClickAction<?> action, Text linkHover) {
			this.action=action;
			this.text=linkHover;
		}
		public ClickAction<?> getAction() { return action; }
		public Text getHoverText() { return text; }
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
				return new ClickActionHolder(TextActions.executeCallback(t->{
					if (t.hasPermission(elem.attr("data-permission")))
						Sponge.getCommandManager().process(t, cmd);
					else
						t.sendMessage(Text.of(TextColors.RED, "You do not have permission to click that link!"));
				}), Text.of(TextColors.GREEN, "¶ Run: ", TextColors.WHITE, cmdVis));
			else
				return new ClickActionHolder(TextActions.runCommand(cmdVis), Text.of(TextColors.GREEN, "Run: ", TextColors.WHITE, cmdVis));
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
				return new ClickActionHolder(TextActions.executeCallback(t->{
					if (t.hasPermission(elem.attr("data-permission")))
						Sponge.getCommandManager().process(Sponge.getServer().getConsole(), cmd);
					else
						t.sendMessage(Text.of(TextColors.RED, "You do not have permission to click that link!"));
				}), Text.of(TextColors.RED, "¶ Server: ", TextColors.WHITE, cmdVis));
			else
				return new ClickActionHolder(TextActions.executeCallback(
						t->Sponge.getCommandManager().process(Sponge.getServer().getConsole(), cmd)),
						Text.of(TextColors.RED, "Server: ", TextColors.WHITE, cmdVis));
		} else if (target.equalsIgnoreCase("_blank")) {
			try {
				return new ClickActionHolder(TextActions.openUrl(new URL(elem.absUrl("href"))), Text.of(TextColors.AQUA, "Extern: ", TextColors.WHITE, elem.attr("href")));
			} catch (Exception e) {
				return new ClickActionHolder(TextActions.executeCallback(activator->activator.sendMessage(Text.of("This link is broken: "+elem.absUrl("href")))), Text.of(TextColors.GRAY, TextStyles.STRIKETHROUGH, "Broken Link"));
			}
		} else if (elem.attr("href").charAt(0)=='#') {
			String page = elem.attr("href").substring(1);
			try {
				int p = Integer.parseInt(page);
				return new ClickActionHolder(TextActions.changePage(p), Text.of(TextColors.GRAY, "Goto page " + p));
			} catch (Exception e) {
				return new ClickActionHolder(TextActions.executeCallback(activator->activator.sendMessage(Text.of("Unknown jump reference: "+elem.attr("href")))), Text.of(TextColors.GRAY, TextStyles.STRIKETHROUGH, "Broken Link"));
			}
		} else {
			return new ClickActionHolder(TextActions.runCommand("/webbook "+elem.absUrl("href")), Text.of(TextColors.GRAY, "Url: ", TextColors.WHITE, elem.attr("href")));
		}
	}
	
	static Website fromUrl(String url, Player player) throws MalformedURLException, IOException {
		Connection con = Jsoup.connect(url).timeout(3000);
		con.header("User-Agent", Configuration.UserAgent);
		if (Configuration.proxy != null) con.proxy(Configuration.proxy);
		con.followRedirects(true);
		Locale playerLocale = player.getLocale();
		Locale serverLocale = Sponge.getServer().getConsole().getLocale();
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

	static Document sendPlayerInfo_PostFormData(Connection con, Player player) throws IOException {
		con.header("Content-Type", "application/x-www-form-urlencoded");
		con.data("Name", player.getName());
		con.data("UUID", player.getUniqueId().toString());
		con.data("World", player.getLocation().getExtent().getName() + "/" + player.getLocation().getExtent().getUniqueId().toString());
		con.data("Location", player.getLocation().getX()+"/"+player.getLocation().getY()+"/"+player.getLocation().getZ());
		con.data("Connection", player.getConnection().getAddress().getHostString()+":"+player.getConnection().getAddress().getPort()+"/"+ player.getConnection().getLatency()+"ms");
		con.data("Joined", player.getJoinData().lastPlayed().get().toEpochMilli()+"/"+player.getJoinData().firstPlayed().get().toEpochMilli());
		con.data("Status", player.get(Keys.HEALTH).orElse(-1.0)+"/"+player.get(Keys.FOOD_LEVEL).orElse(-1)+"/"+player.get(Keys.EXPERIENCE_LEVEL).orElse(-1)+"/"+player.get(Keys.GAME_MODE).orElse(GameModes.NOT_SET).getName());
		return con.post();
	}
	static Document sendPlayerInfo_Header(Connection con, Player player) throws IOException {
		con.header("X-WebBook-User", player.getName() + "; " + player.getUniqueId().toString());
		con.header("X-WebBook-World", player.getLocation().getExtent().getName() + "; " + player.getLocation().getExtent().getUniqueId().toString());
		con.header("X-WebBook-Location", player.getLocation().getX()+"; "+player.getLocation().getY()+"; "+player.getLocation().getZ());
		con.header("X-WebBook-Connection", player.getConnection().getAddress().getHostString()+":"+player.getConnection().getAddress().getPort()+"; "+ player.getConnection().getLatency()+"ms");
		con.header("X-WebBook-Joined", player.getJoinData().lastPlayed().get().toEpochMilli()+"; "+player.getJoinData().firstPlayed().get().toEpochMilli());
		con.header("X-WebBook-Status", player.get(Keys.HEALTH).orElse(-1.0)+"; "+player.get(Keys.FOOD_LEVEL).orElse(-1)+"; "+player.get(Keys.EXPERIENCE_LEVEL).orElse(-1)+"; "+player.get(Keys.GAME_MODE).orElse(GameModes.NOT_SET).getName());
		return con.get();
	}
	static Document sendPlayerInfo_PostJson(Connection con, Player player) throws IOException {
		StringWriter sw = new StringWriter(1024);
		JsonWriter jw = new JsonWriter(sw);
		con.header("Content-Type", "application/json");
		jw.beginObject();
		jw.name("subject").beginObject();
		{
			jw.name("name").value(player.getName());
			jw.name("uuid").value(player.getUniqueId().toString());
			jw.name("health").value(player.get(Keys.HEALTH).orElse(Double.NaN));
			jw.name("foodLevel").value(player.get(Keys.FOOD_LEVEL).map(Double::valueOf).orElse(Double.NaN));
			jw.name("expLevel").value(player.get(Keys.EXPERIENCE_LEVEL).map(Double::valueOf).orElse(Double.NaN));
			jw.name("gameMode").value(player.get(Keys.GAME_MODE).orElse(GameModes.NOT_SET).getName());
		}
		jw.endObject();
		jw.name("location").beginObject();
		{
			jw.name("world").beginObject();
			jw.name("name").value(player.getLocation().getExtent().getName());
			jw.name("uuid").value(player.getLocation().getExtent().getUniqueId().toString());
			jw.endObject();
			jw.name("position").beginObject();
			jw.name("x").value(player.getLocation().getPosition().getX());
			jw.name("y").value(player.getLocation().getPosition().getY());
			jw.name("z").value(player.getLocation().getPosition().getZ());
			jw.endObject();
		}
		jw.endObject();
		jw.name("connection").beginObject();
		{
			jw.name("ip").value(player.getConnection().getAddress().getHostString());
			jw.name("port").value(player.getConnection().getAddress().getPort());
			jw.name("latency").value(player.getConnection().getLatency());
			jw.name("joined").beginObject();
			jw.name("first").value(player.getJoinData().firstPlayed().get().toString());
			jw.name("last").value(player.getJoinData().lastPlayed().get().toString());
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
		if (title != null) website.title = Text.of(title.text());
		else website.title=Text.of("Unnamed");

		doc.select(Configuration.pageSelector).forEach(item->
			website.pages.add(parseNodes(item))
		);
		
		return website;
	}

	String url;
	int recode;
	Map<String, String> reheaders;
	List<Text> pages = new LinkedList<>();
	Text title;
	
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
	public Text getTitle() {
		return title;
	}
	/** All declared pages on this website, where pages are picked with the selector from the configuration.<br>
	 * The default value '<code><tt>ul.book li</tt></code>' would extract <code><tt>document.querySelectorAll("ul.book li")</tt></code>
	 * @return A collection of Text, each representing a page. */
	public Collection<Text> getPages() { return pages; }
	/** Show this website to a player wrapped in a bookview.
	 * @param player the player to show this website to 
	 */
	public void displayBook(Player player) {
		player.sendBookView(BookView.builder().title(title).addPages(pages).build());
	}
	/** Show this website to a player presented by the chat paginator.
	 * @param receiver who ever is supposed to read the website
	 */
	public void displayChat(MessageReceiver receiver) {
		PaginationList.builder()
			.title(title)
			.contents(pages)
			.footer(Text.of(recode + " - " + url))
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
	public ItemStack save(Text author) {
		ItemStack stack = ItemStack.builder().itemType(ItemTypes.WRITTEN_BOOK).quantity(1).build();
		stack.offer(Keys.BOOK_AUTHOR, author);
		stack.offer(Keys.BOOK_PAGES, pages);
		stack.offer(Keys.DISPLAY_NAME, title);
		return stack;
	}
}
