#!/usr/bin/env bash
./gradlew clean assemble
/Users/longpham/Workspace/elasticsearch-5.6.1/bin/elasticsearch-plugin remove elasticsearch-esdocumentsearch
/Users/longpham/Workspace/elasticsearch-5.6.1/bin/elasticsearch-plugin install file:///Users/longpham/Workspace/elastic-search-entity-plugin/build/distributions/elasticsearch-esdocumentsearch-5.6.1.zip
