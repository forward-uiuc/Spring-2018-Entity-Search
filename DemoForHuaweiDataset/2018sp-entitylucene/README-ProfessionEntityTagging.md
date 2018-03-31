# Profession Entity Tagging

This project represents the first steps towards performing fine-grained entity tagging. This means that after an iteration of coarse entity tagging
(e.g. tagging people) with a machine learning technique, contextual clues can be used to narrow down these tags into more specific
categories (e.g. doctor, actor). The project focuses specifically on turining "person" tags into specific professions.

What the project actually does is scrape Wikipedia pages on several different professions for their text, then builds a Statistical Language Model for each.
These language models are normalized by a background language model (https://www.wordfrequency.info/). These are then converted to a single output JSON file
which maps each known word to how relevant it is for each profession.

## Getting Started

These instructions will get you a copy of the project up and running on your local machine for development and testing purposes.

### Installing

####Cloning

First, clone the repo

```
git clone https://username@bitbucket.org/forward-uiuc/2017f-entitylucene.git
```

The relevant project files are in ProfessionEntityTagging.

Create the folders "COMP", "SLMs", and "Professions". Alternatively, populated versions of these files are on Harrier01 at
Harrier01.cs.illinois.edu:/scratch/DatasetArchives/Archives-Semesters/2017F/2017F-EntityLucene/ProfessionEntityTagging

### Demo

To demo the system, simply run OccupationParser.py with python3, then run TopicAnalysis.py with python3. The former will take several minutes.

## Code Overview

OccupationParser.py uses the requests library, and beautifulsoup to scrape several Wikipedia pages. First, it follows a list of links to wikipedia
pages which list different occupations. Once it has URLs to Wikipedia pages for these occupations, it gets the text from each. Optionally, it will
follow a set number of links on the occupation pages to get more related text, up to a set depth of sub links. This can be modified in the code.

TopicAnalaysis.py uses metapy to stem all the words found from Wikipedia, remove stop words, and build a SLM for each profession. It normalizes
the SLMs by a background language model (from https://www.wordfrequency.info/). It can output a JSON file for each profession, or one combined JSON file
(edit the last two lines to change this).