# WebBooks
Open websites in Minecraft

The main command is `/webbook` or `/wbk` for short. Or `/url` if you fancy that.<br>
The syntax is as follows: `/<command> -s -c <url> [target]`.
* `url` is the website to load and display
* `target` is a optional parameter specifying who's supposed to see the website.<br>This parameter requires the permission `webbooks.url.other`
* `-c` will open the website paginated into the chat instead of as a book.<br>following links will open a book reguardless
* `-s` this can not be used with `-c` as it supressed any output. Instead the website will be stored away in a physical book.<br>Links that execute server-commands will not work well with those!<br>This option requires the permission `webbooks.save`
The base permission to use the command is `webbooks.url.command`

###Config

The config file provides options to proxy the requests. This is usefull as every website will be loaded by the game-server. So going to any website will expose the ip to it. Not like you can just lookup the ip by the server-name, but it's there as a feature.<br>Keep in mind tho that any response from the web-server must happen within 3 seconds before timing out!

Additionally you can specify a url to display when a player joins your server. This might be usefull to have a dynamic greeting message, automatically updated rules or what ever you come up with to write on your webserver.

## Web-Part:

Pages are basically selected by `document.querySelector("ul.book").children()`

Example PHP showcasing the required html structure for books to load:
```
<!DOCTYPE html>
<html>
<head>
  <title>Test Book</title>
</head>
<body>
  <ul class="book">
    <li>This is a website!<br><br>
    Your User-Agent: <?= $_SERVER['HTTP_USER_AGENT'] ?>
    <li>Player data we can read:<br>
    <?
      foreach ($_POST as $key => $value) {
        echo "${key}, ";
      }
    ?>
  </ul>
</body>
</html>
```

### Formatting your website

To reflect the limited set for formats available to minecraft a fix set of style classes has to be used.
Those can in return be defined in your stylesheet as well to support displaying the content in an actual web-browser.

The class names have a `mc-`-Prefix followed by the format-code. Some bold red text could be:
```
<span class="mc-c mc-l">Text</span> or
<b><span class="mc-c">Text</span></b> or
<b class="mc-c">Text</b>
```

### Links and special targets

Links will work as expected, loading the website and viewing it as another book.<br>
As you might expect if you add `target="_blank"` to your link Minecraft will ask the player to open the link in the system web-browser.

But it would be pretty boring if that was everything a book could do, so there's a little bit more you can do with links:
* `<a href="#1">` will not jump to sections with the specified id, but instead jump to the given pagenumber
* `<a href="command" target="_player">` will execute the command in href as if the player typed it. the `/` is optional.
* `<a href="command" target="_server">` will execute the command in href as the server with op-powers. These links will break if you save the website, but not cause any big errors (Besides telling the player that the callback stopped working)

### Available player-data:

* `Name: <PlayerName>`
* `UUID: <PlayerUUID>`
* `World: <WorldName>/<WorldUUID>`
* `Location: <X>/<Y>/<Z>`
* `Connection: <IP>:<Port>/<Latency>ms`
* `Joined: <LastJoined>/<FirstJoined>`
* `Status: <Health>/<FoodLevel>/<Level>/<GameMode>`

### User-Agent string:

`<MinecraftName>(<ExecutionType>/<Type>) <MinecraftVersion>/<SpongeName> <SpongeVersion>/WebBooks(webbook) <WebBooksVersion>`

# This Plugin utilizes JSoup
Jsoup is licensed under MIT-License<br>
[Poject Homepage](https://jsoup.org/) &#x2001; [License-Text](https://jsoup.org/license)
