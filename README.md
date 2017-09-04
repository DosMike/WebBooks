# WebBooks
Open websites in Minecraft

### Web-Part:

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
Jsoup is licensed under MIT-License
[Poject Homepage](https://jsoup.org/) [License-Text](https://jsoup.org/license)
