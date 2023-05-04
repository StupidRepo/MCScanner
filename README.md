# MCScanner
Uses masscan and MongoDB to scan for servers and save them to DB.
EDIT/NOTE: I will update this soon so that it saves IPs to a text file instead to skip the hassle with a DB. The whole point of the DB was so that I could make a NodeJS program that lists the servers and stuff in charts on a localhost website.

## Requirements
* `pymongo` - `pip3 install pymongo`
* `mcstatus` - `pip3 install mcstatus`
* `masscan` - Mac/Linux only! (Windows not supported)
* Python 3
* a MongoDB database 

## Install `masscan`
* Install masscan for macOS using `brew`, or `sudo apt get` for Ubuntu
* Install masscan wrapper for python using `pip3 install python-masscan`

## Install a MongoDB database
### macOS
* Run `xcode-select --install` in Terminal
(If you do not have `brew` installed, these instructions won't work until you install `brew`. Please scroll down and read 'Install `brew`' then come back up and run these commands)
* Run `brew tap mongodb/brew`
* Run `brew update`
* Run `brew install mongodb-community@6.0`
* Run `brew services start mongodb-community@6.0`

MongoDB not opening? Check this:
<img width="799" alt="ss" src="https://user-images.githubusercontent.com/69256931/206896713-ce3f5270-c4a0-4f62-b1fa-e2015ff6cdd8.png">
### Linux
Linux has multiple distributions, so find the instructions to install MongoDB for your distribution [here](https://www.mongodb.com/docs/manual/administration/install-on-linux/).

## Install `brew`
### macOS only! (Although you can run `brew` on Linux)
* Run `/bin/bash -c "$(curl -fsSL https://raw.githubusercontent.com/Homebrew/install/HEAD/install.sh)"` in Terminal
* Follow the setup steps
* Make sure it's in your PATH

## Start Scanning
`sudo python3 main.py`
**Make sure you run python3 with sudo or the scanner will *not* work!**

# Warning
### **This program was made for educational purposes only, and is not meant to be used in malicious ways**. 
Do not DDOS the IPs or do *anything* malicious with the IPs. The only thing this program is supposed to be used for is finding random Minecraft servers and joining them!
Oh, and **don't use this program for griefing**. Please.
