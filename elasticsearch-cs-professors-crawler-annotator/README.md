# cs-common-crawl-annotator

#### Crawler for Top 50 Computer Science University departments in the USA

Crawls Computer Science departments of top 50 universities in the USA. Recognizes and types the entities as professor, topic, course, journal, sponsor_agency, conference, money, email, phone, zipcode, date, year, course_number, number, conference_acronym using a combination of bag of words, regular expressions, crowd-sourced knowledge base and named entity recognition techniques.

Specifically, for recognizing professor names, we used a crowd-sourced list of two thousand professors in the US and generated variants of the name to improve the recall such that a name FirstName MiddleName(s) LastName could be one of FirstName LastName, FirstInitial. LastName, FirstInitial LastName, FirstName MiddleInitial(s). LastName, FirstName MiddleInitial(s) LastName etc.

# Example Document

For the following document:
```
Kevin Chang lives in Champaign.
```
The annotator would generate the following annotation for elastic search server.
```
{
  "index": {
    "_type": "d_document"
    }
}

{
  "text": "Kevin Chang lives in Champaign and teaches CS 511.",
  "_entity_professor_begin": "oentityo|000001 Chang lives in Champaign and teaches CS 511.",
  "_entity_professor_end": "Kevin oentityo|000001 lives in Champaign and teaches CS 511.",
  "_entity_course_number_begin": "Kevin Chang lives in Champaign and teaches oentityo|000002 511.",
  "_entity_course_number_end": "Kevin Chang lives in Champaign and teaches CS oentityo|000002.",
}
```

The annotation is used for both entity search and entity semantic document search.

### Citation

1. [CommonCrawl](http://commoncrawl.org)
