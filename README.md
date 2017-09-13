# WebBooks
Open websites in Minecraft

The main command is `/webbook` or `/wbk` for short. Or `/url` if you fancy that.    
The syntax is as follows: `/<command> -s -c <url> [target]`.
* `url` is the website to load and display
* `target` is a optional parameter specifying who's supposed to see the website.<br>This parameter requires the permission `webbooks.url.other`
* `-c` will open the website paginated into the chat instead of as a book.<br>following links will open a book reguardless
* `-s` this can not be used with `-c` as it supressed any output. Instead the website will be stored away in a physical book.<br>Links that execute server-commands will not work well with those!<br>This option requires the permission `webbooks.save`
The base permission to use the command is `webbooks.url.command`

### Config

The config file provides options to proxy the requests. This is usefull as every website will be loaded by the game-server. So going to any website will expose the ip to it. Not like you can just lookup the ip by the server-name, but it's there as a feature.    
Keep in mind tho that any response from the web-server must happen within 3 seconds before timing out!

Additionally you can specify a url to display when a player joins your server. This might be usefull to have a dynamic greeting message, automatically updated rules or what ever you come up with to write on your webserver.

## Web-Part:

Pages are basically selected by `document.querySelector("ul.book").children()`.

Player data are sent using POST data, making it a bit easier to repush GET data from links within the webbook.   
All available Data are listen below and use a slash '/' as data separator, so the location data may look something like this:   
`Location=-820.4/5/723.2` meaning the coordinates are X -820.4, Y 5, Z 723.2

Example PHP showcasing the required html structure for books to load:
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
Using this parameter on "_player" commands will break the link in saves book in the same way "_sever" commands will break in saved books.
An example would be `<a href="stop" target="_server" data-permission="server.admin.stop">Stop the server</a>`

The target along with the url will be shown in a hover text for each link like:
* `URL: url` for normal links
* `Extern: url` for links with target `_blank`
* `Run: command` for links with target `_player`
* `¶ Run: command` for links with target `_player` and a required permission
* `Server: command` for links with target `_server`
* `¶ Server: command` for links with target `_server` and a required permission


### Available player-data:

These data are sent to the server using the POST data

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
Jsoup is licensed under MIT-License    
[Poject Homepage](https://jsoup.org/) &#x2001; [License-Text](https://jsoup.org/license)
