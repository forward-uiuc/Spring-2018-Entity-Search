# Entity Search and Entity-Semantic Document Search

## Summary

[Entity Search](http://vldb.org/conf/2007/papers/research/p387-cheng.pdf) aims at allowing users to search for entities inside documents. And Entity-Semantic Document Search is an extension of Entity Search to allow users to describe documents of interest by describing entities inside them. 

This repository contains an end-to-end prototype system to support Entity Search and Entity-Semantic Document Search on ElasticSearch. It can be considered as the first iteration of the system, which uses [SpanQuery](https://www.elastic.co/guide/en/elasticsearch/reference/5.6/span-queries.html) with a few plugins to support Entity Search and Entity-Semantic Document Search. We plan to make the system more efficient by touching deeper Lucene core, and smarter by improving ranking functions.

## Components

* [elasticsearch-cs-professors-crawler-annotator](https://github.com/forward-uiuc/Spring-2018-Entity-Search/tree/master/elasticsearch-cs-professors-crawler-annotator) contains an annotator to annotate raw data (from 50 CS-department websites) and produce json files to import into ElasticSearch using [Bulk API](https://www.elastic.co/guide/en/elasticsearch/reference/5.6/docs-bulk.html)
* [elastic-search-entity-plugin](https://github.com/forward-uiuc/Spring-2018-Entity-Search/tree/master/elastic-search-entity-plugin) contains a plugin to rewrite user hashtag query, e.g., #professor mining, into span queries, which ElasticSearch can execute, and outputs a list of entities, e.g., professors in the example above, ranked by relevance with contextual keywords, e.g., mining in the example above.
* [elasticsearch-esdocumentsearch-plugin](https://github.com/forward-uiuc/Spring-2018-Entity-Search/tree/master/elasticsearch-esdocumentsearch-plugin) contains a plugin to rewrite user hashtag queries with additional operators, e.g., @near (#professor, #email, #phone), into span queries, which ElasticSearch can execute, and outputs a list of documents ranked by relevance with the entity-semantic query.
* [elasticsearch-analysis-entity-layout/README.md](https://github.com/forward-uiuc/Spring-2018-Entity-Search/blob/master/elasticsearch-analysis-entity-layout/README.md) contains a plugin to override Lucene's term offsets with layout offsets, provided in annotated import files.
* [entity-search-web-interface](https://github.com/forward-uiuc/Spring-2018-Entity-Search/tree/master/entity-search-web-interface) contains a web application supporting both entity search and entity-semantic document search. Users can click a button to switch between the two.
* [entity-search-web-interface](https://github.com/forward-uiuc/Spring-2018-Entity-Search/tree/master/DemoForHuaweiDataset) contains an end-to-end demo for Huawei Q&A dataset. 

Each folder above contains a separate README file.

## Install
- Download Elasticsearch v5.6.1
- \[Optional\] Download Kibana v5.6.1 as the web interface for Elasticsearch
- Import plugins mentioned above (instruction in each folder)
- Run the Elasticsearch and Kibana
- In Kibana (or use CURL in terminal), create the following index mapping, in which each entity is a field (prefix _entity_)  in the document and labled by special token oentityo, which tells us the position of the entity in the original document, which can be used in SpanQuery to detect if an entity of a particular type is near a particular keyword in the document or not:

```
PUT /entity_search_cs_departments/
{
  "mappings": {
    "d_document": {
      "properties": {
        "entityContent": {
          "type": "text",
          "fields": {
            "keyword": {
              "type": "keyword",
              "ignore_above": 256
            }
          }
        },
        "title": {
          "type": "text",
          "fields": {
            "keyword": {
              "type": "keyword",
              "ignore_above": 256
            }
          }
        },
        "url": {
          "type": "text",
          "fields": {
            "keyword": {
              "type": "keyword",
              "ignore_above": 256
            }
          }
        },
        "text": {
          "type": "text",
          "term_vector": "with_positions_offsets_payloads",
          "store": true,
          "analyzer": "fulltext_analyzer"
        }
      },
      "dynamic_templates": [
        {
          "entity_type": {
            "match_mapping_type": "string",
            "match": "_entity_*",
            "mapping": {
              "type": "text",
              "term_vector": "with_positions_offsets_payloads",
              "store": true,
              "analyzer": "entity_analyzer"
            }
          }
        }
      ]
    }
  },
  "settings": {
    "index": {
      "number_of_shards": 1,
      "number_of_replicas": 0
    },
    "analysis": {
      "filter": {
        "keep_entity_word": {
          "type": "keep",
          "keep_words": [
            "oentityo"
          ]
        }
      },
      "analyzer": {
        "entity_analyzer": {
          "type": "custom",
          "tokenizer": "classic",
          "filter": [
            "lowercase",
            "delimited_payload_filter",
             "keep_entity_word"
          ]
        },
        "fulltext_analyzer": {
          "type": "custom",
          "tokenizer": "classic",
          "filter": [
            "lowercase"
          ]
        }
      }
    }
  }
} 
```
- Import data from (elasticsearch-cs-professors-crawler-annotator)[https://github.com/forward-uiuc/Spring-2018-Entity-Search/tree/master/elasticsearch-cs-professors-crawler-annotator] using the following command:

``` 
curl -XPOST localhost:9200/entity_lucene_doc/_bulk -H 'Content-Type: application/json' --data-binary @name-of-import-file.json
```
- If the file is too large, split it into seperate files and XPOST them seperatly.

## Query
- Entity Search (find professors in data mining)

```
GET /entity_search_cs_departments/_search_with_clusters?
{  "search_request" :
 {
  "query": "#professor mining " ,    
  "size":100
   }
}
```
- Entity-Semantic Document Search (find home pages of professors in data mining)
```
GET /entity_search_cs_departments/_es_document_search?
{
  "search_request":{
    "query": "@near ( #professor #email #phone ) @contains ( mining )",
    "size" : 100
  } 
}
```
