package de.dosmike.sponge.WebBooks;

import ninja.leaping.configurate.ConfigurationNode;
import ninja.leaping.configurate.commented.CommentedConfigurationNode;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.text.format.TextColors;
import org.spongepowered.api.text.serializer.TextSerializers;

import java.net.InetSocketAddress;
import java.net.Proxy;

public class Configuration {
    static String motd=null;
    static boolean extendedTooltips = true;
    static Text defaultAuthor = Text.of(TextColors.AQUA, "Saved Website");
    static String pageSelector = "ul.book li";
    static String transportMethod = "post/json";
    static Proxy proxy;
    static String UserAgent="WebBooks/INITIALIZING (Minecraft-Server Plugin by DosMike)";

    static void reload() {
        try {
            ConfigurationNode s = WebBooks.getInstance().configManager.load();
            if (s.getNode("TransportMethod").isVirtual()) {
                s.mergeValuesFrom(generateDefault());
                WebBooks.getInstance().configManager.save(s);
            }

            String host = s.getNode("Proxy", "Host").getString("");
            int port = s.getNode("Proxy", "Port").getInt(8080);
            if (host.isEmpty()) proxy = null;
            else proxy = new Proxy(Proxy.Type.HTTP, new InetSocketAddress(host, port));

            motd = s.getNode("MOTD").getString("");

            extendedTooltips = s.getNode("ExtendedTooltip").getBoolean(true);

            defaultAuthor = TextSerializers.FORMATTING_CODE.deserialize(s.getNode("DefaultAuthor").getString("&bSaved Website"));

            pageSelector = s.getNode("PageSelector").getString("ul.book li");

            transportMethod = s.getNode("TransportMethod").getString("post/json");
            if (!transportMethod.equalsIgnoreCase("post/json") &&
                !transportMethod.equalsIgnoreCase("post/formdata") &&
                !transportMethod.equalsIgnoreCase("get/header"))
                throw new IllegalArgumentException("The specified TransportMethod was invalid - post/json will be used");

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    static CommentedConfigurationNode generateDefault() {
        CommentedConfigurationNode root = WebBooks.getInstance().configManager.createEmptyNode();
        root.getNode("Proxy").setComment("Here you can set a HTTP-Proxy for the Game-Server to use when connecting to the Web-Server. Note: The overall delay must not exceed 3 seconds!");
        root.getNode("Proxy", "Host").setValue("");
        root.getNode("Proxy", "Host").setComment("Leave this value empty to disable the proxy");
        root.getNode("Proxy", "Port").setValue(8080);
        root.getNode("MOTD").setComment("Specify a URL here that will be displayed to every player that joins the server");
        root.getNode("ExtendedTooltip").setValue(true);
        root.getNode("ExtendedTooltip").setComment("If you set this to false, players won't see action information on tooltips for links");
        root.getNode("PageSelector").setValue("ul.book li");
        root.getNode("PageSelector").setComment("Imagine this CSS-Selector being passed to document.querySelectorAll() to collect the page contents");
        root.getNode("DefaultAuthor").setValue("&bSaved Website");
        root.getNode("DefaultAuthor").setComment("Change the name used by default, when saving a book");
        root.getNode("TransportMethod").setValue("post/json");
        root.getNode("TransportMethod").setComment("This specifies the way player information is sent to the server.\nPossible values are post/json, post/formdata and get/headers. For more information check the Ore or Git page ");
        return root;
    }

}
