# MCScanner
Uses masscan and MongoDB to scan for servers and save them to DB.

### Requirements
* `pymongo` - `pip3 install pymongo`
* `mcstatus` - `pip3 install mcstatus`
* `masscan` - Mac/Linux only! (Windows not supported) `pip3 install python-masscan`
* Python 3
* a MongoDB database

### Start Scanning
`sudo python3 main.py`
**Make sure you run python3 with sudo or the scanner will *not* work!**
