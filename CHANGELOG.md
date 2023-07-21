# Changes
All notable changes to this project will be documented in `CHANGELOG.md`.

## Added
* Search bar to the Server List.
* Ability to search by IP, MOTD, Version and Max Players!
* `PlaceholderText` for easy `JTextField` placeholder text.
* `DatabaseHandler.updateServerByIPInDB()` for updating a server in the DB by it's IP.

## Modified
* Disable selection and editing on the JTable/Server List.
* Scanning code YET again to fix it with resuming!

## Removed
* The thing that tells you how many IPs are left to scan.
  * It was inaccurate and I'm pretty sure getting & drawing that number to the screen made it lag a lot.
* Clicking a row to copy it's IP.
* `DatabaseHandler.writeDetailsToDB(String ip, String motd, Integer maxPlayers)` because I can just make the version number report "1.6<="

## TODOs
- [ ] Optimise IP generation and initial scanning code.[¹][1]
- [ ] Optimise code in general.
- [ ] Separate tool for viewing servers in DB. (code is already here (`ServerList` class), just need to make it separate)
- [x] ~~Make it save what IP it got to when quitting so that it can resume from that IP on next startup.~~
- [x] ~~Add a GUI for viewing servers in a nice friendly grid.~~

[1]: https://github.com/StupidRepo/MCScanner/blob/main/src/com/stupidrepo/mcscanner/MCScanner.java#L126