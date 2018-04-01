import argparse
import json
import csv
import os
import re
import sys
import string
import urllib2
import html2text

from string import maketrans
from bs4 import BeautifulSoup

ENTITY_TYPES = ["professor", "topic", "course", "journal", "sponsor_agency", "conference", "money", "email", "phone", "zipcode", "date", "year", "course_number", "conference_acronym"]
ENTITY_DICTS = {}
html_filepath2url = {}
html_filepath2md_filepath = {}

for entity in ENTITY_TYPES:
    l = []
    with open('files/' + entity + '_list.txt', 'rb') as csvfile:
        csv_reader = csv.reader(csvfile, delimiter=',')
        for row in csv_reader:
            if row and len(row) > 0:
                l.append(row[0])

    l.sort(key=lambda x: len(x), reverse=True)
    ENTITY_DICTS[entity] = l

domains = os.listdir('data')
for domain in domains:
    documents  = os.listdir(os.path.join('data', domain, 'html'))

    # index URL and TITLE corresponding to markdown file
    urls_filepath = os.path.join('data', domain, 'URLS.csv')
    with open(urls_filepath, 'rb') as urls_file:
        urls_csv = csv.reader(urls_file, delimiter=',')
        for row in urls_csv:
            if row and len(row) > 0:
                html_filepath2url[row[1]] = row[0]
                html_filepath2md_filepath[row[1]] = row[2]

    for doc in documents:
        print doc
        doc_path = os.path.join('data', domain, 'html', doc)
        with open(doc_path, 'rb') as html_file:
            try:
                html = html_file.read().decode("utf8")
                fp = open(html_filepath2md_filepath[doc_path], "w")
            except:
                sys.stderr.write("[***] ERROR skipping %s because of codec decode error\n" % doc)
                continue

            h = html2text.HTML2Text()
            h.ignore_links = True
            h.ignore_images = True

            content = h.handle(html).encode('utf-8')
            trantab = maketrans('_#*|[]', '      ')
            content = content.translate(trantab)
            content = content.replace('\n', '. ')
            # printable = set(string.printable)
            # content = ''.join([ch for ch in content if ch in printable else ' '])
            content = ''.join([i if ord(i) < 128 else ' ' for i in content])
            # content = content.replace('\n', '. ').replace('_', ' ').replace('#', ' ').replace('*', ' ').replace('|', ' ').replace('[', ' ').replace(']',' ')
            content = ' '.join(content.split())
            fp.write(content)

            try:
                pass
            except:
                sys.stderr.write("[*] ERROR: Failed to write markdown for page %s\n" % doc_path)
                fp.close()
                continue

            fp.close()

            document_entity_ctr = 0
            annotation = {'text':content, 'url': html_filepath2url[doc_path]}

            try:
                soup = BeautifulSoup(urllib2.urlopen(annotation['url']), "lxml")
                annotation['title'] = soup.title.string
            except:
                sys.stderr.write("[*] ERROR Title not found for %s\n" % doc_path)
                annotation['title'] = "Title not found"

            for entity in ENTITY_TYPES:
                text = content
                entity_hash2entity_value = {}

                for pattern in ENTITY_DICTS[entity]:
                    pattern = pattern.strip()
                    try:
                        if entity == "professor" or pattern.isupper():
                            matches = [(m.start(), m.end()) for m in re.finditer(pattern, text)]
                        else:
                            matches = [(m.start(), m.end()) for m in re.finditer(pattern, text, re.IGNORECASE)]
                        matches = [(start,end) for (start,end) in matches if (text[max(0,start-1)].isalnum() or text[min(len(text)-1,end)].isalnum()) == False]
                    except:
                        continue
                        # sys.stderr.write("MATCH FAILED for %s type and %s pattern\n" % (entity, pattern))
                    if len(matches) == 0:
                        continue
                    # sys.stderr.write("Matches for %s are %s\n" % (pattern, str([text[start-5:end+5] for (start, end) in matches])))
                    ctr = document_entity_ctr + len(matches)
                    document_entity_ctr += len(matches)

                    for start, end in matches[::-1]:
                        ctr -= 1
                        entity_hash2entity_value[ctr] = text[start:end]
                        sys.stderr.write("Found entity: %s and text: %s\n" % (entity, entity_hash2entity_value[ctr]))
                        text = text[:start] + 'oentityo|' + str(ctr).zfill(6) + text[end:]

                if text != content:
                    text = ' '.join(text.split())
                    text_begin = text
                    text_end = text

                    entities = entity_hash2entity_value.keys()
                    entities.sort(reverse=True)
                    # print entities

                    for hash in entities:
                        entity_name = entity_hash2entity_value[hash]
                        entity_name_split = entity_name.split()
                        entity_split_len = len(entity_name_split)
                        if entity_split_len > 1:
                            hash = str(hash).zfill(6)
                            # sys.stderr.write("%s " % entity_name)
                            # sys.stderr.write("Replacing begin %s with %s" % ('oentityo|'+str(hash), 'oentityo|'+str(hash)+' '+' '.join(entity_name_split[1:])))
                            # sys.stderr.write("Replacing end %s with %s\n" % ('oentityo|'+str(hash), ' '.join(entity_name_split[:entity_split_len-1]) + ' oentityo|'+str(hash)))
                            text_begin = text_begin.replace('oentityo|'+str(hash), 'oentityo|'+str(hash)+ entity_name[len(entity_name_split[0]):], 1)
                            text_end = text_end.replace('oentityo|'+str(hash), entity_name[:len(entity_name)-len(entity_name_split[-1])] + 'oentityo|'+str(hash), 1)

                    annotation['_entity_'+entity+'_begin'] = text_begin
                    annotation['_entity_'+entity+'_end'] = text_end
                    # if not (content.count(' ') == text_begin.count(' ') == text_end.count(' ')):
                    #     sys.stderr.write('\n\n\n\t%s\n%d\n\n\n\t%s\n%d\n\n\n\t%s\n%d\n\n\n' % (content, content.count(' '), text_begin, text_begin.count(' '), text_end, text_end.count(' ')))

            annotation_path = os.path.join('data', domain, 'annotation.json')
            with open(annotation_path, "a") as annotation_file:
                annotation_file.write('{"index": {"_type": "d_document"}}\n')
                annotation_file.write(json.dumps(annotation))
                annotation_file.write('\n')
