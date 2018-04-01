
This is a plugin to enable multiple entity search and clustering on elastic search, for any data with annotated entities (works both for apple dataset and new academia dataset.), indexed in elastic search. 
For more details and structure of plugins in elastic search see this - 
https://www.elastic.co/guide/en/elasticsearch/plugins/current/index.html
Information is provided for different kinds of plugins – discovery, analysis, mapper , ingest and store plugin. Also, information is given on how to develop and maintain the plugins.
Install:
To install the plugin:

Go to the plugin folder and run the following commands on windows (*for linux and mac remove the .bat extension)
.\gradlew.bat clean assemble
Then go to the folder where you have installed elastic search and go to bin 
Run following commands – (*ignore .bat extension for linux and mac)
```
.\elasticsearch-plugin.bat remove elasticsearch-carrot2
.\elasticsearch-plugin.bat install file:///[[path to elastic search plugin folder]]\elastic-search-entity-plugin\build\distributions\elasticsearch-carrot2-5.6.1.zip
```

Modify code:
For query parsing and clustering, look into ClusteringAction.java in
```
https://github.com/forward-uiuc/Spring-2018-Entity-Search/blob/master/elastic-search-entity-plugin/src/main/java/org/entitysearch/elasticsearch/ClusteringAction.java
```
Query parsing example: 
```
Given query - #hardware #software memory memory
Ouput query -
GET entity_lucene_doc/_search?
{
  "query": {
    "span_near": {
      "clauses": [
        {
          "field_masking_span": {
            "query": {
              "span_term": {
                "_entity_general_hardware_begin": "oentityo"
              }
            },
            "field": "text"
          }
        },
        {
          "field_masking_span": {
            "query": {
              "span_term": {
                "_entity_software_begin": "oentityo"
              }
            },
            "field": "text"
          }
        },
        {
          "span_term": {
            "text": "memory"
          }
        },
        {
          "span_term": {
            "text": "memory"
          }
        }
      ],
      "slop": 10,
      "in_order": false
    }
  }
}

```
Clustering means that similar entity instances are gathered, scored and scores aggregated.

Configure project properties in:
```
/src/main/config 
```

Run:


After installing the plugin in elastic search instance it can be accessed from kibana or web interface (Other sample queries on web interface).
From kibana use queries like:
```
GET /entity_search_cs_departments/_search_with_clusters?
{  "search_request" :
 {
  "query": "#professor mining " ,    
  "size":100
   }
}
```

Algorithms: 
1.  Convert given query into elastic search format. 
2.  Retrieve docs elastic search
3.  Find entity instances in docs (Do post processing)
4.  Score and rank the entity outputs
  a.  Proximity based scoring is implemented for queried entities and context words
  b.  Results for all occurrences of the entity globally are added, to take frequency into account
5.  Send results to web interface
6.  Limit retrieved relevant doc number to100 (configurable)
7.  Considered context window to reduce relevant entities, it faster than previous version, need to use interval tree to make it better. 


Future work:
1.  Better clustering provided the expansion rules for name of professor and other entities(academic dataset)
2.  Incremental results in interface, better user experience.
3.  Go deep into elastic search to make this plugin a part of the search system. And ensure our fundamentals assumptions are satisfied from search engine itself. 
4. Even after using context window, the plugin is slow in post processing. Faster than version 1 though. Need to invert on offset of entity and add interval tree for faster processing. 
