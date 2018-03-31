import requests
import time
from bs4 import BeautifulSoup

#A recursive function to scrape a wikipedia page for its text contents. It will also scrape some linked pages.
#url to the wikipedia page on the profession
#link_count is the number of links to follow on a given page
#depth is how many more times subpages will be scraped
def get_words(url, link_count, depth):
    print(url + "-" + str(depth))
    next_page_links = []
    page_text = ""
    profession_page = requests.get(url).text
    time.sleep(0.5)
    profession_soup = BeautifulSoup(profession_page, "lxml")
    profession_bodysoup = profession_soup.find('div', {'id': 'body-content'})
    if profession_bodysoup is None:
        profession_bodysoup = profession_soup.find('div', {'id': 'bodyContent'})
    if profession_bodysoup is None:
        profession_bodysoup = profession_soup
    for paragraph in profession_bodysoup.find_all('p'):
        page_text += paragraph.text
    all_next_links = (link.get("href") for link in profession_bodysoup.find_all('a'))
    for next_linkstub in all_next_links:
        if next_linkstub is not None and "/wiki/" in next_linkstub and\
                        '.' not in next_linkstub and '?' not in next_linkstub and ':' not in next_linkstub:
            next_page_links.append("https://en.wikipedia.org" + next_linkstub)
    next_page_links = list(set(next_page_links))
    if depth > 1:
        for link in next_page_links:
            page_text += get_words(link, link_count, depth-1)
            link_count -= 1
            if link_count <= 0:
                return page_text
    return page_text

#Gets URLs to professions
def generate_professions():
    professions = {}
    for profession_list_url in list_urls:
        print(profession_list_url)
        page = requests.get(profession_list_url).text
        time.sleep(0.5)
        soup = BeautifulSoup(page, "lxml")
        bodysoup = soup.find('div', {'id': 'body-content'})
        if bodysoup == None:
            bodysoup = soup.find('div', {'id': 'bodyContent'})
        if bodysoup == None:
            bodysoup = soup
        all_links = (link.get("href") for link in bodysoup.find_all('a'))
        for linkstub in all_links:
            if linkstub != None and "/wiki/" in linkstub and '.' not in linkstub and '?' not in linkstub and ':' not in linkstub:
                prof_title = linkstub[linkstub.index("/")+1:]
                prof_title = prof_title[prof_title.index("/") + 1:]
                professions[prof_title] = "https://en.wikipedia.org" + linkstub
    return professions


list_urls = ["https://en.wikipedia.org/wiki/List_of_artistic_occupations",
                 "https://en.wikipedia.org/wiki/List_of_dance_occupations",
                 "https://en.wikipedia.org/wiki/List_of_film_and_television_occupations",
                 "https://en.wikipedia.org/wiki/List_of_theatre_personnel",
                 "https://en.wikipedia.org/wiki/List_of_writing_occupations",
                 "https://en.wikipedia.org/wiki/List_of_corporate_titles",
                 "https://en.wikipedia.org/wiki/List_of_industrial_occupations",
                 "https://en.wikipedia.org/wiki/List_of_metalworking_occupations",
                 "https://en.wikipedia.org/wiki/List_of_railway_industry_occupations",
                 "https://en.wikipedia.org/wiki/List_of_sewing_occupations",
                 "https://en.wikipedia.org/wiki/Category:Law_enforcement_occupations",
                 "https://en.wikipedia.org/wiki/List_of_computer_occupations",
                 "https://en.wikipedia.org/wiki/List_of_scientific_occupations",
                 "https://en.wikipedia.org/wiki/List_of_healthcare_occupations",
                 "https://en.wikipedia.org/wiki/List_of_mental_health_occupations",
                 "https://en.wikipedia.org/wiki/List_of_nursing_specialties"
                 ]

profession_links = generate_professions()

print("Starting word gathering")

for profession in profession_links.keys():
    print(profession)
    profession_text = get_words(profession_links[profession], 2, 2)
    profession_text = profession_text.replace("\t", " ")
    profession_text = profession_text.replace("\n", " ")
    outfile = open("Professions/"+ profession + ".txt", 'w+')
    outfile.write(profession_text)
    outfile.close()