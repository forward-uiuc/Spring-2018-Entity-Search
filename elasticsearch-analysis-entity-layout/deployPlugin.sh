#!/usr/bin/env bash
mvn package -DskipTests
mvn clean package -DskipTests
/Users/longpham/Workspace/elasticsearch-5.6.1/bin/elasticsearch-plugin remove elasticsearch-analysis-entity-layout
/Users/longpham/Workspace/elasticsearch-5.6.1/bin/elasticsearch-plugin install file:///Users/longpham/Workspace/elasticsearch-analysis-entity-layout/target/releases/elasticsearch-analysis-entity-layout-5.6.1.zip
