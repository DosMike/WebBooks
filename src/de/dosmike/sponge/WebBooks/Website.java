package de.dosmike.sponge.WebBooks;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.Proxy;
import java.net.URL;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.jsoup.Connection;
import org.jsoup.Jsoup;
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
import org.spongepowered.api.text.format.TextColor;
import org.spongepowered.api.text.format.TextColors;
import org.spongepowered.api.text.format.TextFormat;
import org.spongepowered.api.text.format.TextStyle;
import org.spongepowered.api.text.format.TextStyles;

public class Website {
	static String UserAgent="Minecraft-Server/WebBooks unknown";
	static Proxy proxy;
	
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
	
	private static Text parseNodes(Node node) {
		if (node instanceof TextNode) {
			return Text.of(((TextNode) node).text());
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
				hoverText.add(0, Text.of(elem.attr("title")));
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
		private ClickAction<?> action;
		private Text text;
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
			String cmd = elem.attr("href"); if (cmd.charAt(0)!='/') cmd='/'+cmd; 
			return new ClickActionHolder(TextActions.runCommand(cmd), Text.of(TextColors.GREEN, "Run: ", TextColors.WHITE, elem.attr("href")));
		} else if (target.equalsIgnoreCase("_server")) {
			return new ClickActionHolder(TextActions.executeCallback(activator->{
				Sponge.getCommandManager().process(Sponge.getServer().getConsole(), elem.attr("href"));
			}), Text.of(TextColors.RED, "Server: ", TextColors.WHITE, elem.attr("href")));
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
		con.header("User-Agent", UserAgent);
		if (proxy != null) con.proxy(proxy);
		con.data("Name", player.getName());
		con.data("UUID", player.getUniqueId().toString());
		con.data("World", player.getLocation().getExtent().getName() + "/" + player.getLocation().getExtent().getUniqueId().toString());
		con.data("Location", player.getLocation().getX()+"/"+player.getLocation().getY()+"/"+player.getLocation().getZ());
		con.data("Connection", player.getConnection().getAddress().getHostString()+":"+player.getConnection().getAddress().getPort()+"/"+ player.getConnection().getLatency()+"ms");
		con.data("Joined", player.getJoinData().lastPlayed().get().toEpochMilli()+"/"+player.getJoinData().firstPlayed().get().toEpochMilli());
		con.data("Status", player.get(Keys.HEALTH).orElse(-1.0)+"/"+player.get(Keys.FOOD_LEVEL).orElse(-1)+"/"+player.get(Keys.EXPERIENCE_LEVEL).orElse(-1)+"/"+player.get(Keys.GAME_MODE).orElse(GameModes.NOT_SET).toString());
		con.followRedirects(true);
		Document doc = con.post();
		return parseDocument(doc, con.response().statusCode(), con.response().headers());
	}
	static Website fromHtml(String html, String baseUrl, Player player) {
		return parseDocument(Jsoup.parse(html, baseUrl), 200, new HashMap<>());
	}
	private static Website parseDocument(Document doc, int rc, Map<String, String> headers) {
		Website website = new Website();
		website.url = doc.baseUri();
		website.recode = rc;
		website.reheaders = headers;
		Element title = doc.getElementsByTag("title").first();
		if (title != null) website.title = Text.of(title.text());
		else website.title=Text.of("Unnamed");
		
		doc.getElementsByTag("ul").forEach(list->{
			if (list.classNames().contains("book")) {
				list.children().forEach(item->{
					website.pages.add(parseNodes(item));
				});
			}
		});
		
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
	/** All declared pages on this website, where pages are children of the first &lt;ul&gt; element with class book.<br>
	 * The JavaScript equivalent for selecting Pages would be <pre>document.querySelector("ul.book").children();</pre>
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
		ItemStack stack = ItemStack.builder().itemType(ItemTypes.WRITTEN_BOOK).quantity(1).build();
		stack.offer(Keys.BOOK_AUTHOR, Text.of(TextColors.DARK_AQUA, "Saved Website"));
		stack.offer(Keys.BOOK_PAGES, pages);
		stack.offer(Keys.DISPLAY_NAME, title);
		return stack;
	}
}
