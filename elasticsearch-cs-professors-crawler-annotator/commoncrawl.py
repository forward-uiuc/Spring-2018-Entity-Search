import requests
import argparse
import time
import json
import StringIO
import gzip
import csv
import codecs
import string
import os
import html2text

from bs4 import BeautifulSoup

import sys
reload(sys)
sys.setdefaultencoding('utf8')

"""CONFIG"""
# CONFIG OUTPUT DIRECTORY
OUT_DIRECTORY = 'data'

# list of available indices
INDEX_LIST = ["2018-09", "2018-05", "2017-51", "2017-47", "2017-43", "2017-39", "2017-34", "2017-30", "2017-26", "2017-22", "2017-17", "2017-13", "2017-09", "2017-04", "2016-50", "2016-44", "2016-40", "2016-36", "2016-30", "2016-26", "2016-22", "2016-18", "2016-07", "2015-48", "2015-40", "2015-35", "2015-32", "2015-27", "2015-22", "2015-18", "2015-14", "2015-11", "2015-06", "2014-52", "2014-49", "2014-42", "2014-41", "2014-35", "2014-23", "2014-15", "2014-10", "2013-48", "2013-20"]
TEST_INDEX_LIST = ["2018-09", "2018-05"]

index_list = INDEX_LIST

# parse the command line arguments
ap = argparse.ArgumentParser()
ap.add_argument("-d", "--domain", required=True, help="The domain to target eg. cnn.com")
ap.add_argument("-r", "--resume", help="Resume from last session or re-run", action='store_true')
args = vars(ap.parse_args())

# read list of domains from file
if args['domain'].endswith(('.csv', '.txt')):
    domains = []

    with open(args['domain'], 'rb') as csvfile:
        csv_reader = csv.reader(csvfile, delimiter=',')
        for row in csv_reader:
            domains.append(row[0])

else:
    domains = [args['domain']]

# read list of file extensions to filter
file_exts = []
with open('files/filtered-file-extensions-list.txt', 'rb') as csvfile:
    csv_reader = csv.reader(csvfile, delimiter=',')
    for row in csv_reader:
        file_exts.append(row[0])
file_exts = tuple(file_exts)


def search_domain(domain):
    """
    Searches all Common Crawl Indices for a domain.
    """
    record_list = []
    seen_urls = {}
    sys.stderr.write ("[**] Trying target domain: %s\n" % domain)

    for index in index_list:
        sys.stderr.write ("[*] Trying index %s\n" % index)

        cc_url = "http://index.commoncrawl.org/CC-MAIN-%s-index?" % index
        cc_url += "url=%s&matchType=domain&output=json" % domain

        response = requests.get(cc_url)

        if response.status_code == 200:

            records = response.content.splitlines()

            for record in records:
                record = json.loads(record)
                url = record['url']
                if not url.endswith((file_exts)) and url not in seen_urls:
                    seen_urls[record['url']] = True
                    record_list.append((record, index_list.index(index)))

            sys.stderr.write("[*] Found %d records\n" % len(records))

    sys.stderr.write("[*] Found a total of %d hits\n" % len(record_list))

    return record_list


def download_page(record):
    """
    Downloads a page from Common Crawl.
    Adapted graciously from @Smerity -
    https://gist.github.com/Smerity/56bc6f21a8adec920ebf
    """

    offset, length = int(record['offset']), int(record['length'])
    offset_end = offset + length - 1

    # We'll get the file via HTTPS so we don't need to worry about S3 credentials
    # Getting the file on S3 is equivalent however - you can request a Range
    prefix = 'https://commoncrawl.s3.amazonaws.com/'

    # We can then use the Range header to ask for just this set of bytes
    resp = requests.get(prefix + record['filename'],
                        headers={'Range': 'bytes={}-{}'.format(offset, offset_end)})

    try:
        # The page is stored compressed (gzip) to save space
        # We can extract it using the GZIP library
        raw_data = StringIO.StringIO(resp.content)
        f = gzip.GzipFile(fileobj=raw_data)

        # What we have now is just the WARC response, formatted:
        data = f.read()

    except IOError:
        sys.stderr.write("[**] ERROR: Failed to read page %s\n" % record['url'])
        return ""

    response = ""

    if len(data):
        try:
            warc, header, response = data.strip().split('\r\n\r\n', 2)
            status_code = int(header.split('\n', 1)[0].split()[1])
            if status_code == 200:
                sys.stderr.write("[*] Retrieved %d bytes from %s\n" % (len(response), record['url']))
                return response

            else:
                sys.stderr.write("[**] ERROR: Page %s returned with status code %d.\n" % (record['url'], status_code))
                return ""
        except:
            pass

    return ""


def format_filename(s):
    """
    Take a string and return a valid filename constructed from the string.
    Uses a whitelist approach: any characters not present in valid_chars are
    removed. Also spaces are replaced with underscores.
    """
    s = s.replace('/', '_')
    valid_chars = "-_.() %s%s" % (string.ascii_letters, string.digits)
    filename = ''.join(c for c in s if c in valid_chars)
    filename = filename.replace(' ','_')
    return filename


def store_session_info(key, val):
    """
    Writes val to hidden file.
    """
    file = open('.session.' + key, 'w')
    file.write(str(val))
    file.close()


def get_session_info(key):
    """
    Reads session info from hidden file.
    """
    file = open('.session.' + key, 'r')
    val = file.readline().rstrip('\n')
    file.close()
    return val


"""
For all indices, crawl domain and download HTML.
"""

# setup output data directory
if not os.path.exists(OUT_DIRECTORY):
    os.makedirs(OUT_DIRECTORY)

# config resume from last session
resume_from_last_run = args['resume']
if resume_from_last_run:
    prev_domain = get_session_info('domain')
    prev_index = int(get_session_info('index'))
    resume_domain = True
    resume_index = True

for domain in domains:
    if resume_from_last_run and resume_domain:
        if domain == prev_domain:
            resume_domain = False
        else:
            continue

    # keep track of current session domain
    store_session_info('domain', domain)

    record_list = search_domain(domain)

    domain_data_dir = os.path.join('data', format_filename(domain))
    # create domain data directory, if one does not exist
    if not os.path.exists(domain_data_dir):
        os.makedirs(domain_data_dir)
        os.makedirs(os.path.join(domain_data_dir, 'html'))
        os.makedirs(os.path.join(domain_data_dir, 'markdown'))
        os.makedirs(os.path.join(domain_data_dir, 'text'))

    # write record URLs in domain
    url_filepath = os.path.join(domain_data_dir, 'URLS.csv')
    url_fp = open(url_filepath, "a")
    # write header to URL file if file is empty
    if os.stat(url_filepath).st_size == 0:
        url_fp.write('url,html filepath, markdown filepath, text filepath\n')

    curr_index_pos = -1
    for record, index_pos in record_list:
        # if resume from last run, then skip records until index_pos is same as prev_index
        if resume_from_last_run and index_pos < prev_index:
            continue
        elif resume_from_last_run and resume_index:
            sys.stderr.write("[**] Resuming from last run with domain %s and index %s\n" % (domain, INDEX_LIST[index_pos]))
            resume_index = False

        # keep track of current session domain
        if curr_index_pos != index_pos:
            curr_index_pos = index_pos
            store_session_info('index', curr_index_pos)

        html_content = download_page(record)

        if html_content != "":
            record_filename = format_filename(record['url'])
            record_html_filepath = os.path.join(domain_data_dir, 'html', record_filename)
            if not record_html_filepath.endswith('.html'):
                record_html_filepath += '.html'
            try:
                fp = open(record_html_filepath, "w")
            except IOError:
                sys.stderr.write("[***] ERROR: Document skipped because file name is too long: %s\n" % record['url'])
                continue

            fp.write(html_content)
            fp.close()

            record_markdown_filepath = os.path.join(domain_data_dir, 'markdown', record_filename)
            if not record_markdown_filepath.endswith('.md'):
                record_markdown_filepath += '.md'
            fp = open(record_markdown_filepath, "w")
            try:
                fp.write(html2text.html2text(html_content))
            except:
                sys.stderr.write("[***] ERROR: Failed to write markdown for page %s\n" % record['url'])
            fp.close()

            record_text_filepath = os.path.join(domain_data_dir, 'text', record_filename)
            if not record_text_filepath.endswith('.txt'):
                record_text_filepath += '.txt'
            fp = open(record_text_filepath, "w")
            soup = BeautifulSoup(html_content, "html5lib")
            content = soup.get_text().replace('\n','\n\n')
            fp.write(content)
            fp.close()

            url_fp.write(record['url'] + ',' + record_html_filepath + ',' + record_markdown_filepath + ',' + record_text_filepath + '\n')

    url_fp.close()
