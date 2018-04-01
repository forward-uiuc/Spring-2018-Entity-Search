import html2text

from os import path
from selenium import webdriver
from bs4 import BeautifulSoup as bs

data_path = 'data'
domain_path = 'forwarddatalab.org'
filename = 'http__www.forwarddatalab.org_people'

html_path = path.join(data_path, domain_path, 'html', filename + '.html')
txt_path = path.join(data_path, domain_path, 'txt', filename + '.txt')
md_path = path.join(data_path, domain_path, 'md', filename + '.md')

file = open(html_path, 'r')
html = file.read()
file.close()

driver = webdriver.PhantomJS()
driver.get("data:text/html;charset=utf-8," + html)

soup = bs(html, 'html5lib')
[x.extract() for x in soup.findAll('script')]

rendered_html =  soup.prettify()

file = open(txt_path, 'w')
file.write(str(soup))
file.close()

file = open(md_path, 'w')
try:
    file.write(html2text.html2text(rendered_html))
except:
    sys.stderr.write("[***] ERROR: Failed to write markdown.\n")
file.close()
