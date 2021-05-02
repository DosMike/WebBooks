package de.dosmike.sponge.WebBooks;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.spongepowered.configurate.CommentedConfigurationNode;
import org.spongepowered.configurate.ConfigurationNode;
import org.spongepowered.configurate.serialize.SerializationException;

import java.net.InetSocketAddress;
import java.net.Proxy;

public class Configuration {
    static String motd=null;
    static boolean extendedTooltips = true;
    static Component defaultAuthor = Component.text("Saved Website", NamedTextColor.AQUA);
    static String pageSelector = "ul.book li";
    static String transportMethod = "post/json";
    static Proxy proxy;
    static String UserAgent="WebBooks/INITIALIZING (Minecraft-Server Plugin by DosMike)";

    static void reload() {
        try {
            ConfigurationNode s = WebBooks.getInstance().configManager.load();
            if (s.node("TransportMethod").virtual()) {
                s.mergeFrom(generateDefault());
                WebBooks.getInstance().configManager.save(s);
            }

            String host = s.node("Proxy", "Host").getString("");
            int port = s.node("Proxy", "Port").getInt(8080);
            if (host.isEmpty()) proxy = null;
            else proxy = new Proxy(Proxy.Type.HTTP, new InetSocketAddress(host, port));

            motd = s.node("MOTD").getString("");

            extendedTooltips = s.node("ExtendedTooltip").getBoolean(true);

            defaultAuthor = LegacyComponentSerializer.legacyAmpersand().deserialize(s.node("DefaultAuthor").getString("&bSaved Website"));

            pageSelector = s.node("PageSelector").getString("ul.book li");

            transportMethod = s.node("TransportMethod").getString("post/json");
            if (!transportMethod.equalsIgnoreCase("post/json") &&
                !transportMethod.equalsIgnoreCase("post/formdata") &&
                !transportMethod.equalsIgnoreCase("get/header"))
                throw new IllegalArgumentException("The specified TransportMethod was invalid - post/json will be used");

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    static CommentedConfigurationNode generateDefault() throws SerializationException {
        CommentedConfigurationNode root = WebBooks.getInstance().configManager.createNode();
        root.node("Proxy")
                .comment("Here you can set a HTTP-Proxy for the Game-Server to use when connecting to the Web-Server. Note: The overall delay must not exceed 3 seconds!");
        root.node("Proxy", "Host")
                .set("")
                .comment("Leave this value empty to disable the proxy");
        root.node("Proxy", "Port")
                .set(8080);
        root.node("MOTD")
                .comment("Specify a URL here that will be displayed to every player that joins the server");
        root.node("ExtendedTooltip")
                .set(true)
                .comment("If you set this to false, players won't see action information on tooltips for links");
        root.node("PageSelector")
                .set("ul.book li")
                .comment("Imagine this CSS-Selector being passed to document.querySelectorAll() to collect the page contents");
        root.node("DefaultAuthor")
                .set("&bSaved Website")
                .comment("Change the name used by default, when saving a book");
        root.node("TransportMethod")
                .set("post/json")
                .comment("This specifies the way player information is sent to the server.\nPossible values are post/json, post/formdata and get/headers. For more information check the Ore or Git page ");
        return root;
    }

}
