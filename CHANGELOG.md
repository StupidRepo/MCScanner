# Changes
All notable changes to this project will be documented in `CHANGELOG.md`.
## Added
* Resuming function!

## Modified
* ScannerThread checks if IP is in DB before even making a Socket for the IP.

## Removed
Nothing has been removed.

## TODOs
- [ ] Optimise IP generation and inital scanning code.[1]
- [ ] Optimise code in general.
- [ ] Seperate tool for viewing servers in DB. (code is already here (`ServerList` class), just need to make it seperate)
- [x] ~~Make it save what IP it got to when qutting so that it can resume from that IP on next startup.~~
- [x] ~~Add a GUI for viewing servers in a nice friendly grid.~~

[1]: https://github.com/StupidRepo/MCScanner/blob/main/src/com/stupidrepo/mcscanner/MCScanner.java#L126