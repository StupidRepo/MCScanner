# Changes
All notable changes to this project will be documented in `CHANGELOG.md`.

## Added
Nothing has been added.

## Modified
* `ServerList` refreshes every 10 seconds.
* Fixed bug where when an offset got to ~252-255, it would stay there and miss out on ***a lot*** of IPs.

## Removed
* The thing that tells you how many IPs are left to scan. It was inaccurate and I'm pretty sure getting & drawing that number to the screen made it lag a lot.

## TODOs
- [ ] Optimise IP generation and initial scanning code.[¹][1]
- [ ] Optimise code in general.
- [ ] Separate tool for viewing servers in DB. (code is already here (`ServerList` class), just need to make it separate)
- [x] ~~Make it save what IP it got to when quitting so that it can resume from that IP on next startup.~~
- [x] ~~Add a GUI for viewing servers in a nice friendly grid.~~

[1]: https://github.com/StupidRepo/MCScanner/blob/main/src/com/stupidrepo/mcscanner/MCScanner.java#L126