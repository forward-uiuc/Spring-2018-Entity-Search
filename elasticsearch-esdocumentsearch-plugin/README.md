# Entity-Semantic Document Search

## Intro
This plugin extends Entity Search to support Entity-Semantic Document Search by providing more operators, such as @near, @contains, and @layout_near in the hash-tag queries. For example, users can type ``` @near(#professor, #email, #phone) @contains(mining) ``` to search for homepages of professors who work in Data Mining. Users can type of the queries for the same task. The idea is we want to provide users with expressive operators so that users can define the documents they look for in a semantic manner.

Essentially, the idea is to parse user query in our predefined language, e.g., ``` @near(#professor, #email, #phone) @contains(mining) ``` , into the format where ElasticSearch can execute, e.g., [SpanQuery](https://www.elastic.co/guide/en/elasticsearch/reference/5.6/span-queries.html), and with information about predefined layout of the index.

For more details and structure of plugins in elastic search see this - https://www.elastic.co/guide/en/elasticsearch/plugins/current/index.html Information is provided for different kinds of plugins – discovery, analysis, mapper , ingest and store plugin. Also, information is given on how to develop and maintain the plugins.

## Install
To install the plugin:
Go to the plugin folder and run the following commands on linux or mac machine (if you use a windows machine, please find the way to install ES plugins on internet):

First, we need to compile the code:
```
./gradlew clean assemble
```

Then, we need to remove the plugin if beeing installed previously:
```
path-to-elasticsearch-5.6.1/bin/elasticsearch-plugin remove elasticsearch-esdocumentsearch
```

Then, install the plugin:
```
path-to-elasticsearch-5.6.1/bin/elasticsearch-plugin install file://path-to-plugin/target/releases/elasticsearch-esdocumentsearch-5.6.1.zip
```

Please note that, in order for the plugin to work, we need to define index schema as below and annotate data as being described in [elasticsearch-cs-professors-crawler-annotator](https://github.com/forward-uiuc/Spring-2018-Entity-Search/tree/master/elasticsearch-cs-professors-crawler-annotator), which can be imported automatically by [Bulk API](https://www.elastic.co/guide/en/elasticsearch/reference/5.6/docs-bulk.html):

```
PUT /entity_lucene_dinv_new_analysis/
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
        "name": {
          "type": "text",
          "fields": {
            "keyword": {
              "type": "keyword",
              "ignore_above": 256
            }
          }
        },
        "physicalDoc": {
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
        }, 
        {
          "entity_type": {
            "match_mapping_type": "string",
            "match": "_xpos_entity_*",
            "mapping": {
              "type": "text",
              "term_vector": "with_positions_offsets_payloads",
              "analyzer": "xpos_entity_analyzer"
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
        "my_stopwords": {
          "type": "stop",
          "stopwords": [
            "the",
            "a"
          ]
        },
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
        "xpos_entity_analyzer": {
          "type": "custom",
          "tokenizer": "layout_tokenizer",
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
            "lowercase",
            "my_stopwords"
          ]
        }
      }
    }
  }
}
```

## Run
With the plugin, you can run a query as below (with a new request handler _es_document_search):

```
GET /entity_search_cs_departments/_es_document_search?
{
  "search_request":{
    "query": "@near ( #course #number )",
    "size" : 1000,
    "explain" : true
  } 
}
```

## To understand the code

The plugin is action plugin, which means it extends Elasticsearch’s runtime action by adding a customized RESTful endpoint called _es_document_search.

The plugin contains a restful handler which has customized response handler and request handler. There we can get user query and restructure it into the format we want: [ClusteringAction.java](https://github.com/forward-uiuc/Spring-2018-Entity-Search/blob/master/elasticsearch-esdocumentsearch-plugin/src/main/java/org/entitysearch/elasticsearch/ClusteringAction.java)

## Future work

* Add more operators such as near corner, etc.
* Create another plugin to customize ranking function
* Add support for Natural Language Querying
