import sys
import requests
import json
import os

def main():
    page = 0
    if len(sys.argv) == 2:
        page = sys.argv[1]

    print("page: " + page)

    api_key = "946bcb8c-ba44-432d-b1ab-c0ddc948c4cb"
    base_url = "https://api.transitfeeds.com/v1/"
    get_feeds = "/getFeeds"

    pay_load = {'key': api_key, 'page': page, 'limit': 10, 'type': 'gtfs'}
    request = requests.get(base_url + get_feeds, params = pay_load);
    response = json.loads(request.text)

    feeds = response['results']['feeds']

    jobs = []
    for feed in feeds:
        if 't' in feed and 'u' in feed and 'd' in feed['u']:
            job = {'title': feed['t'],'url': feed['u']['d']}
            jobs.append(job)

    for job in jobs:
        title = "'" + job['title'] + "'".encode('utf8')
        url = job['url'].encode('utf8')
        print(title)
        print(url)
        os.system('mkdir ' + title)
        os.system("gtsql -p " + title + " -d " + title + "/test.db -u " + url)
        print("\n\n")

if __name__ == "__main__":
    main()
