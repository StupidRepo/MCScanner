# MCScanner
Now re-made in Java, and not Python!

## Requirements
* [`JDK 17`][adopt17]
* MongoDB database (required for storing IPs)
* Stable internet connection
* A good computer (with a good CPU)

## Run
### macOS + Linux + Windows
1. Click [here][latest] to download the latest tested, safe, and stable build.
2. Run the `.jar` file with `java -jar /path/to/MCScanner.jar` from Command Prompt or Terminal.

> **Note**<br>
> If you're on macOS, you can just double-click on the `.jar` file.

## Build
### macOS + Linux + Windows
1. Click [here][latest-source] to download untested, unsafe & unstable source code.
2. Open it in IntelliJ IDEA.
3. Build the project with `Build > Build Project`.
4. Run the project with `Run > Run 'MCScanner [run]'`.

## Contributing
### General
If you want to contribute to this project, you can fork this repository, make your changes, and then make a pull request!
### Languages
To contribute to the languages, fork the repository, analyse the `en-GB.json` file, make a new file with your language locale (see [this][lang]), translate the values (not the keys), and then make a pull request!
Please don't use Google Translate :(

## Changelogs
All notable changes to this project will be documented in [`CHANGELOG.md`][changes].

## Warning
> **Warning**<br>
> This program was made for **educational purposes only**, and is **not meant to be used in malicious ways**. 
> **Do not DDOS the IPs** or do *anything* malicious with the IPs.
> The only thing this program is supposed to be used for is finding random Minecraft servers and joining them, to have fun!

Oh, and **don't use this program for griefing**. Please.

[adopt17]: https://adoptium.net/en-GB/download/

[changes]: https://github.com/StupidRepo/MCScanner/blob/main/CHANGELOG.md
[latest]: https://github.com/StupidRepo/MCScanner/releases/latest/download/MCScanner.jar
[latest-source]: https://github.com/StupidRepo/MCScanner/archive/refs/heads/main.zip

[lang]: https://www.oracle.com/java/technologies/javase/jdk17-suported-locales.html