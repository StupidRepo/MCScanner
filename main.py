import json
import masscan
from mcstatus import JavaServer
import random
import concurrent.futures
import pymongo

ok = False

threads = 4

while True:
    try:
        threads = int(input("How many threads should I use (recommended: 4)? "))
        if threads > 32:
            print("Going above 32 threads will:\nuse so much of your router's bandwidth that it'll slow the whole "
                  "internet down (and slow the program down)\nslow down your computer "
                  "significantly or crash it.")
        break
    except:
        print("Not a valid number!")

myclient = pymongo.MongoClient("mongodb://localhost:27017/")

mydb = myclient["MCScanner"]
mycol = mydb["servers"]

A = list(range(1, 0xff))
B = list(range(1, 0xff))

random.shuffle(A)
random.shuffle(B)

ip_ranges = []

for a in A:
    for b in B:
        ip_range = f"{a}.{b}.0.0/16"
        ip_ranges.append(ip_range)

def thread(ipr):
    status = None
    mas = masscan.PortScanner()
    mas.scan(ipr, ports='25565', arguments='--max-rate 105000')
    scan_result = json.loads(mas.scan_result)
    for ip in scan_result['scan']:
        print("Possible server...")
        server = JavaServer(ip, 25565)
        status = server.status()
        print("Found a server! Saving to DB!")
        j = {
            'ip': server.address.host,
            'description': status.description,
            'version': status.version.name,
            'online_players': status.players.online,
            'max_players': status.players.max
        }
        if mycol.count_documents({'ip': server.address.host}, limit=1) != 0:
            print("Server already exists in DB! Updating!")
            mycol.update_one({'ip': server.address.host}, j)
        else:
            mycol.insert_one(j)
            print("Saved to DB!")

if __name__ == '__main__':
    print(f"=====STARTING SCAN=====")

    random.shuffle(ip_ranges)

    executor = concurrent.futures.ThreadPoolExecutor(threads)
    for ip_range in ip_ranges:
        idk = executor.submit(thread, ip_range)
