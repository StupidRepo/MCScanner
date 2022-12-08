import json
import masscan
from mcstatus import JavaServer
import random
import concurrent.futures
import pymongo
import time
import sys

servers_found = 0

threads = 4
seed = random.randrange(sys.maxsize)

text = False

print("WARNING! Using or modifying the code in any way to turn it into\n"
      "a DDos program is illegal! This program was made for educational purposes, "
      "and should not be used in malicious ways! This includes:\nLeaking/selling server IPs, DDosing people or"
      "Doxxing people is illegal. Do not use this program with malicious intent!")
print("I made this program for EDUCATIONAL PURPOSES only. If you try to use this program to DDos someone,"
      "then you are responsible for what happens. Not me!")
print("Make sure to please read the Minecraft terms and the EULA here:"
      "\nhttps://www.minecraft.net/en-us/terms\nhttps://www.minecraft.net/en-us/eula")
print("Remember, using this program maliciously is illegal! DO NOT DDos people!")
print("(Oh and also, if you are playing on a multiplayer whilst running this, you will disconnect!")
print()

while True:
    try:
        threads = int(input("How many threads should I use (default & recommended: 4)? "))
        if threads > 32:
            print("Going above 32 threads will:\n- use so much of your router's bandwidth that it'll slow your "
                  "router down (and slow the program down)\n- slow down your computer "
                  "significantly\nOR\n- crash it.")
        break
    except:
        break

while True:
    try:
        seedLocal = input("What seed should the random generator use (default: system time)? ")
        if seedLocal.strip().__len__() != 0:
            seed = seedLocal
        break
    except:
        break

rng = random.Random(seed)

myclient = pymongo.MongoClient("mongodb://localhost:27017/")

mydb = myclient["MCScanner"]
mycol = mydb["servers"]

A = list(range(1, 0xff))
B = list(range(1, 0xff))

rng.shuffle(A)
rng.shuffle(B)

ip_ranges = []

for a in A:
    for b in B:
        ip_range = f"{a}.{b}.0.0/16"
        ip_ranges.append(ip_range)

def doPrintLoop():
    while True:
        count = mycol.estimated_document_count()
        start = count - servers_found
        print(f'\rScanning for servers. Found: {servers_found} '
              f'{"server" if servers_found == 1 else "servers" }. '
              f'Servers in DB at start: {start} '
              f'{"server" if start == 1 else "servers" }, '
              f'servers in DB now: {count} {"server" if count == 1 else "servers" }. '
              f'(+{round((count - start) / count * 100, 2)}%)',
              end=' ', flush=True)
        time.sleep(.5)

def thread(ipr):
    mas = masscan.PortScanner()
    mas.scan(ipr, ports='25565', arguments='--max-rate 105000')
    scan_result = json.loads(mas.scan_result)
    for ip in scan_result['scan']:
        server = JavaServer(ip, 25565)
        status = server.status()
        j = {
            'ip': server.address.host,
            'description': status.description,
            'version': status.version.name,
            'online_players': status.players.online,
            'max_players': status.players.max,
        }
        global servers_found
        servers_found += 1
        if mycol.count_documents({'ip': server.address.host}, limit=1) != 0:
            mycol.update_one({'ip': server.address.host}, j)
        else:
            mycol.insert_one(j)

if __name__ == '__main__':
    print(f"=====STARTING SCAN=====")
    print(f'SEED: {seed}')

    rng.shuffle(ip_ranges)

    executor = concurrent.futures.ThreadPoolExecutor(threads)
    for ip_range in ip_ranges:
        idk = executor.submit(thread, ip_range)

    if not text:
        executor2 = concurrent.futures.ThreadPoolExecutor(1)
        executor2.submit(doPrintLoop)
        text = True
