# WebBooks
Open websites in Minecraft

The main command is `/webbook` or `/wbk` for short. Or `/url` if you fancy that.    
The syntax is as follows: `/<command> [-s [-a <author>] | -c] <url> [target]`.
* `url` is the website to load and display
* `target` is a optional parameter specifying who's supposed to see the website.   
This parameter requires the permission `webbooks.url.other`
* `-c` will open the website paginated into the chat instead of as a book.   
following links will open a book reguardless
* `-s` this can not be used with `-c` as it supressed any output. Instead the website will be stored away in a physical book.   
Links that execute server-commands will not work well with those!<br>This option requires the permission `webbooks.url.save`
* `-a <author>` only affects `-s`. Will set the author of the book item. Supports color codes.
   This option requires the permission `webbooks.url.author`

The base permission to use the command is `webbooks.url.base`

In order to browse domains, the permission `webbooks.broswe.<domain>` is required as well. The permission requires the domain backwards to allow the permission system to automatically manage subdomains. For example: `example.com` would require the permission `webbooks.browse.com.example`. That permission would also allow `minecraft.example.com`. And due to how the permission system works `webbooks.browse` handles all domains.

### Config

The config file provides options to `Proxy` the requests. This is usefull as every website will be loaded by the game-server. So going to any website will expose the ip to it. Not like you can just lookup the ip by the server-name, but it's there as a feature.    
Keep in mind tho that any response from the web-server must happen within 3 seconds before timing out!

With the `MOTD` you can specify a url to display when a player joins your server. This might be usefull to have a dynamic greeting message, automatically updated rules or what ever you come up with to write on your webserver.

The `ExtendedTooltips` option is on by default. It add an extra line to ever link, showing the command that will be run or the URL that will be opened. Tooltips can also be hidden per link (see the following section).

Specifying a `DefaultAuthor` is only required if you don't like the default author ('Saved Website') when `-s` is used to save the website to an item.

The `PageSelector` is probably the most important part in your project, as it determines how pages are split up. Minecraft has no way of doing automatic page breaks unfortunately, and this solution seems fine. The default selector is `ul.book li`.

Various player information is sent to the server, depending on the specified `TransportMethod`.   
Valid values are described below.

## Web-Part:

Pages are by default selected with the css-selector `ul.book li`. This can be changed in the config tho, if you don't like it.

Player data are sent according to the transport method. If you're writing a service and you want to be able to service all types, you can distinguish them by request method and Content-Type header.

Example PHP showcasing how to server `post/formdata` request with the default `ul.book li` selector:
```
<?PHP
//Preparing data for display, split them on the delimiter '/'
$worldData = explode('/', $_POST['World']);
$worldName = $worldData[0];

$statusData = explode('/', $_POST['Status']);
$health = $statusData[0];
$level = $statusData[2];

$playerName = $_POST['Name'];
?>
<!DOCTYPE html>
<html>
<head>
  <title>Test Book</title>
</head>
<body>
  <ul class="book">
    <li><u>This is <span class="mc-m">website</span> book!</u>
    <br>
    <br>Your User-Agent: <?= $_SERVER['HTTP_USER_AGENT'] ?>
    <br>
    <br><i>1</i> <a href="#2">2</a> <a href="#3">3</a>

    <li>Hello, <?= $playerName ?> level <?= $level ?>
    <br>
    <br>You are currently in <?= $worldName ?> with <?= $health ?> HP
    <br>
    <br><a href="#1">1</a> <i>2</i> <a href="#3">3</a>

    <li>Test some links:
    <br>
    <br><a href="kill" target="_player">Die now</a>
    <br><a href="test.php">Reload site</a>
    <br><a href="stop" target="_server" data-permission="webbooks.links.admin" title="Please dont :<">Kill the server</a>
    <br><a href="http://www.google.com" target="_blank">Go to google</a>
    <br>
    <br><a href="#1">1</a> <a href="#2">2</a> <i>3</i>
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

### Adding hover text

You can attach a line of text to be displayed on hovering to pretty much any element using the `title` attribute for the html node in case a element needs further explanation.

### Links and special targets

Links will work as expected, loading the website and viewing it as another book.    
As you might expect if you add `target="_blank"` to your link Minecraft will ask the player to open the link in the system web-browser.

But it would be pretty boring if that was everything a book could do, so there's a little bit more you can do with links:
* `<a href="#1">` will not jump to sections with the specified id, but instead jump to the given pagenumber
* `<a href="command" target="_player">` will execute the command in href as if the player typed it. the `/` is optional.
* `<a href="command" target="_server">` will execute the command in href as the server with op-powers. These links will break if you save the website, but not cause any big errors (Besides telling the player that the callback stopped working)

Links with target "_player" and "_server" allow for a additional attribute `data-permission` to restrict usage.
Using this parameter on "_player" commands will break the link in saved book in the same way "_sever" commands will break in saved books.
An example would be `<a href="stop" target="_server" data-permission="server.admin.stop">Stop the server</a>`

The target along with the url will be shown in a hover text for each link like:
* `URL: url` for normal links
* `Extern: url` for links with target `_blank`
* `Run: command` for links with target `_player`
* `¶ Run: command` for links with target `_player` and a required permission
* `Server: command` for links with target `_server`
* `¶ Server: command` for links with target `_server` and a required permission

If you don't want this target text you can disable `ExtendedTooltips` in the config or add the attribute `data-title-hide-href`.


### Available player-data:

For `post/json` the request method will be POST and player data will be a json like this:
```json
{
  "subject": {
    "name": String, "uuid": UUID,
    "health": Number, "foodLevel": Number,
    "expLevel": Number, "gameMode": String
  },
  "location": {
    "world": { "name": String, "uuid": UUID },
    "position": { "x": Number, "y": Number, "z": Number }
  },
  "connection": {
    "ip": String, "port": Number, "latency": Number,
    "joined": { "first": IsoDate, "last": IsoDate }
  }
}
```
For `post/formdata` the request method will be POST and the data will be url-form endcoded. (New Lines are only for readability)
```
Name=<NAME>&
UUID=<UUID>&
World=<NAME>/<UUID>&
Location=<X>/<Y>/<Z>&
Connection=<IP>:<PORT>/<LATENCY>ms&
Joined=<FIRST>/<LAST>&
Status=<HEALTH>/<FOODLEVEL>/<EXPLEVEL>/<GAMEMODE>
```
For `get/header` the request method will be GET and the data will be sent in headers:
```
X-WebBook-User=<NAME>; <UUID>
X-WebBook-World=<NAME>; <UUID>
X-WebBook-Location=<X>; <Y>; <Z>
X-WebBook-Connection=<IP>:<PORT>; <LATENCY>ms
X-WebBook-Joined=<FIRST>; <LAST>
X-WebBook-Status=<HEALTH>; <FOODLEVEL>; <EXPLEVEL>; <GAMEMODE>
```

### User-Agent string:

##### Before 1.2:
`<MinecraftName>(<ExecutionType>/<Type>) <MinecraftVersion>/<SpongeName> <SpongeVersion>/WebBooks(webbook) <WebBooksVersion>`
##### Since 1.2:
`<MinecraftName>/<MinecraftVersion> <SpongeName>/<SpongeVersion>(<SpongePlatform>; <SpongeType>) WebBooks/<WebBooksVersion> (webbook; by DosMike)`

# This Plugin utilizes JSoup
Jsoup is licensed under MIT-License    
[Poject Homepage](https://jsoup.org/) &#x2001; [License-Text](https://jsoup.org/license)
