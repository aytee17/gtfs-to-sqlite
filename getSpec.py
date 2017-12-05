from bs4 import BeautifulSoup
import urllib2
import pprint
import json
import re

page = urllib2.urlopen('http://gtfs.org/reference/').read()
soup = BeautifulSoup(page, "html.parser")

heading_tags = soup.find_all('h3')
file_names = [str(x.string) for x in heading_tags]

tbody = soup.find_all('tbody')

d = {}

for i in range(1, len(tbody)):
	key = str(tbody[i].parent.find_previous_sibling('h3').string)
	if key in d:
		continue
	l = []

	for row in tbody[i].find_all('tr'):
		# the first column
		attr = row.find('td')
		name = unicode(attr.string)

		if name.isspace(): 
			continue

		l.append(name)

	d[key] = l

pp = pprint.PrettyPrinter(depth=3)
pp.pprint(d)

print len(d)

json_string = str(json.dumps(d))


json_string = re.sub(r'\",', '\": \"\",', json_string)
json_string = re.sub(r'\"]', '\": \"\"]', json_string)
json_string = re.sub(r'\[', '{', json_string)
json_string = re.sub(r'\]', '}', json_string)

print json_string

parsed = json.loads(json_string)
json_string = json.dumps(parsed, indent=4)

writeFile = open('GTFS_Specification', 'w')
writeFile.write(json_string)
writeFile.close()




	