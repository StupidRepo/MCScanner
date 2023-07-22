# Changes
All notable changes to this project will be documented in `CHANGELOG.md`.

## Added
* A [LanguageHandler][langhand] for handling strings in different languages. If you want to add a new language, read [this][readme-lang].

## Modified
Nothing has been modified.

## Removed
Nothing has been removed.

## TODOs
- [ ] Optimise IP generation and initial scanning code.
- [ ] Optimise code in general.
- [ ] Separate tool for viewing servers in DB. (code is already here (`ServerList` class), just need to make it separate)
- [x] ~~Make it save what IP it got to when quitting so that it can resume from that IP on next startup.~~
- [x] ~~Add a GUI for viewing servers in a nice friendly grid.~~

[readme-lang]: https://github.com/StupidRepo/MCScanner/tree/main#languages
[langhand]: https://github.com/StupidRepo/MCScanner/blob/main/src/com/stupidrepo/mcscanner/language/LanguageHandler.java