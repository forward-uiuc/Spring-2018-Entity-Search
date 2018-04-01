# Elastic Search Analysis Plugin for Entity Layout Info

## Intro
This plugin allows us to index entities' visual positions on the page in ElasticSearch. It is particularly useful for entity-semantic document search because very often relative or absolute positions of entities are useful to describe a page of interest. For example, professor homepages can be described as pages where emails, phone numbers are visually near a photo, and they appear inside the top one third of the page.

Essentially, the idea is inject layout information to term during the annotation process (by adding that info as a suffix for a term separated by a delimiter, e.g., "oentityo|15"), and then read that info to override offset values of terms. By doing so, we basically cast default offset values of terms into their layout offset. To enable this, we create an analysis plugin to create a customized tokenizer called "layout_tokenizer", which can read injected-layout information for term.

For more details and structure of plugins in elastic search see this - 
https://www.elastic.co/guide/en/elasticsearch/plugins/current/index.html
Information is provided for different kinds of plugins – discovery, analysis, mapper , ingest and store plugin. Also, information is given on how to develop and maintain the plugins.

## Install
To install the plugin:
Go to the plugin folder and run the following commands on linux or mac machine (if you use a windows machine, please find the way to install ES plugins on internet):

First, we need to compile the code:
```
mvn package -DskipTests
mvn clean package -DskipTests
```

Then, we need to remove the plugin if beeing installed previously:
```
path-to-elasticsearch-5.6.1/bin/elasticsearch-plugin remove elasticsearch-analysis-entity-layout
```

And finally install the plugin:
```
path-to-elasticsearch-5.6.1/bin/elasticsearch-plugin install file://path-to-plugin/target/releases/elasticsearch-analysis-entity-layout-5.6.1.zip
```

## Extend code

If you want to modify the code, you need to compile/install the plugin and restart ElasticSearch.

The logic is mainly in [EntityLayoutTokenizer.java](https://github.com/forward-uiuc/Spring-2018-Entity-Search/blob/master/elasticsearch-analysis-entity-layout/src/main/java/org/forward/entitysearch/entitylayoutanalysis/EntityLayoutTokenizer.java) and the tokenizer is registered in [AnalysisEntityLayoutPlugin.java](https://github.com/forward-uiuc/Spring-2018-Entity-Search/blob/master/elasticsearch-analysis-entity-layout/src/main/java/org/forward/entitysearch/entitylayoutanalysis/AnalysisEntityLayoutPlugin.java)

## Run
After installing the plugin in elastic search instance, now you can use a customized tokenizer called "layout_tokenizer"
```
"xpos_entity_analyzer": {
          "type": "custom",
          "tokenizer": "layout_tokenizer",
          "filter": [
            "lowercase",
            "delimited_payload_filter",
             "keep_entity_word"
          ]
        }
```

## Future work
1.  Test this plugin with big annotated dataset. Now, it is tested with only a toy dataset.
2.  Explore how to fundamentally increase the number of offsets for each term, so we can have multiple types of offsets for different terms.
3.  Explore how to allow users query jointly on multiple types of offsets for each term, which is similar to what R-Tree does for spacial data.
