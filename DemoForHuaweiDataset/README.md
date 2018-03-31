# Demo for Huawei Dataset
This is the entire demo from HUAWEI	dataset. Here is the workflow to setup the project on your local machine:

## Annotation and indexing
Inside the folder '2018sp-entitylucene', the sub-part is used for annotating and indexing the input plain text which contains questions and answers. The dataset for input is inside the folder 'EntitySearch'

## Import the Data into Elasticsearch
- Download the Elasticsearch v5.6.1. Follow the instruction online to install it on local machine.
- Download the Kibana v5.6.1 as the interface for Elasticsearch.
- Run the Elasticsearch and Kibana on local machine
- In Kibana, create the index mapping as:
```
PUT /entity_lucene_doc/
{
  "mappings": {
    "document": {
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
          "tokenizer": "whitespace",
          "filter": [
            "lowercase",
            "type_as_payload",
            "delimited_payload_filter",
            "keep_entity_word"
          ]
        },
        "fulltext_analyzer": {
          "type": "custom",
          "tokenizer": "whitespace",
          "filter": [
            "lowercase",
            "type_as_payload",
            "my_stopwords"
          ]
        }
      }
    }
  }
}
```
- cd into the folder that contains the esdata0.json

- In terminal, type:
```
curl -XPOST localhost:9200/entity_lucene_doc/_bulk -H 'Content-Type: application/json' --data-binary @esdata0.json
```
- If the file is too large, split it into seperate files and XPOST them seperatly.

## Install ElasticSearch Plugin
Inside the folder 'ElasticsearchPlugin', follow the instruction to install the plugin.

## Deploy WebInterface
Inside the folder 'EntityQAWebInterface', follow the insruction to install the backend and frontend, and start them seperatly.

* [NERDocumentAnnotator](./2018sp-entitylucene/NERDocumentAnnotator/README.md)

* [ElasticSearch Plugin](./ElasticsearchPlugin/README.md)

* [EntityQAWebInterface](./EntityQAWebInterface/README.md)

* [OutputGenerator](./OutputGenerator/README.md)
