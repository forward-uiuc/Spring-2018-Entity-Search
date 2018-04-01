from selenium import webdriver
from bs4 import BeautifulSoup as bs
from bs4.element import Comment
import itertools
import csv

def xpath_soup(element):
    """
    Generate xpath of soup element.
    :param element: bs4 text or node.
    :return: xpath as string.
    """
    components = []
    child = element if element.name else element.parent
    for parent in child.parents:
        """
        @type parent: bs4.element.Tag
        """
        previous = itertools.islice(parent.children, 0, parent.contents.index(child))
        xpath_tag = child.name
        xpath_index = sum(1 for i in previous if i.name == xpath_tag) + 1
        components.append(xpath_tag if xpath_index == 1 else '%s[%d]' % (xpath_tag, xpath_index))
        child = parent
    components.reverse()
    return '/%s' % '/'.join(components)


def write_coord_csv(html):

    seen = set()

    file = open("out.csv", "w")
    file.write("text_content, top_left, top_right, bottom_left, bottom_right")
    file.write("\n")

    driver = webdriver.PhantomJS()
    driver.set_window_size(1120, 550)
    driver.get(html)

    soup = bs(html, "lxml")
    [x.extract() for x in soup.findAll('script')]

    for elem in soup.find_all(text=True):
        
        if len(elem.split()) > 0:
            try:
                xpath = xpath_soup(elem)
                if xpath not in seen:
                    seen.add(xpath)
                    element = driver.find_element_by_xpath(xpath)
                    area = element.size["width"] * element.size["height"]
                    if area > 0:
                        text_content = element.text.replace("\n", " ").replace("\r", " ").replace("\"", "&quot")

                        if len(text_content) > 1:
                            top_left = (element.location["x"], element.location["y"])
                            top_right = (element.location["x"] + element.size["width"], element.location["y"])
                            bottom_left = (element.location["x"], element.location["y"] + element.size["height"])
                            bottom_right = (element.location["x"] + element.size["width"], element.location["y"] + element.size["height"])
                            line = "\"{0}\",\"{1}\",\"{2}\",\"{3}\",\"{4}\"".format(text_content, top_left, top_right, bottom_left, bottom_right)

                            seen.add(text_content)

                            file.write(line)
                            file.write("\n")
            except:
                continue

    file.close()


if __name__ == "__main__":
    file = open("/Users/zubin/Desktop/Forward/cs-common-crawl/data/forwarddatalab.org/html/http__www.forwarddatalab.org_kevinchang.html", "r")
    html = file.read()
    file.close()
    write_coord_csv(html)



'''
for elem in soup.find_all("img"):
    #print xpath_soup(elem)
    xpath = xpath_soup(elem)
    if xpath.find("noscript") < 0:
        element = driver.find_element_by_xpath(xpath)
        area = element.size["width"] * element.size["height"]
        if area > 0:
            print xpath
            print element.location
            print element.size
            #print element.text
'''
