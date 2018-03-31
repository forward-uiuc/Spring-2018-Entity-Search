# Named Entity Recognition Document Annotator

This project provides a framework for performing NER on documents and generating files ready for upload to Elastic Search for entity aware search.

## Getting Started

These instructions will get you a copy of the project up and running on your local machine for development and testing purposes.

### Installing

####Cloning
The following instructions are for setting up Lucene Entity Search in Eclipse

First, clone the repo into your local machine

In Eclipse, use File > Open Projects From Filesystem...
Import NERDocumentAnnotator and lucene-solr-master/lucene.

####Build Paths
Next you have to configure the build paths.

NERDocumentAnnotator

* Begin by rightclicking NERDocumentAnnotator in the Package Explorer
    * Build Path > Configure Build Path OR Properties > Java Build Path
* In the Libraries tab, remove any jars not in the JRE System Library
* Click Add External Jars...
    * Add all jars in the NERLuceneDependencies folder
* Click Apply
 

####Datasets

The input of Huawei dataset can be found at: /path/to/project/DemoForHuaweiDataset/EntitySearch/StandardQuestion_QuestionAnswer_Tagging.txt
The dictionary input is located at the folder: /path/to/project/DemoForHuaweiDataset/EntitySearch/dictionaries

####Demo

To configure the demo, open [LOCALPATHS_ANNOTATOR.txt](/LOCALPATHS_ANNOTATOR.txt)

Note that the paths used here must be absolute paths.

* Edit the "HUAWEIDOCS:/path/to/huawei/folder" line to point to a file of "documents" in Huawei's format. In our case, it should point to StandardQuestion_QuestionAnswer_Tagging.txt file as described in the above Datasets
* Edit the "HUAWEIDICT:/path/to/dictionary/files" line to point to the folder containing the Huawei annotator's dictionary files as described in the above Datasets
* Edit the "CLASSIFIER:/path/to/project/20178sp-entitylucene/NERLuceneDependencies/english.all.3class.distsim.crf.ser.gz" line to point to the Stanford classifier.
* Edit the "POSTAGGER:/path/to/project/2018sp-entitylucene/NERLuceneDependencies/english-bidirectional-distsim.tagger" line to point to the Stanford Part of Speech tagger.



You must also adjust src/AnnotationConfig.txt. This allows for different NER options. Brief explanations are in the file.
Importantly, this file you must specify an entity ontology. This ontology represents the structure of allowed entity types to be tagged. The specification is in JSON, and takes the format
{"EntityType0": ["Subtype0", "Subtype1"], "Subtype0": ["Subsubtype0, ...], ...}


Open [NERDocumentAnnotator/src/Manager.java](NERDocumentAnnotator/src/Manager.java) to select the number of documents to annotate and their style of annotation.
The options include
* switching between the Huawei style documents and stack exchange style
* switching between generating JSON files for import to Elastic Search and serialized data structures for NERLucene
* switching between entity-inverted and entity aware document search semantics
* switching between splitting entity types into separate indexes and combing them (splitting between indexes is likely broken at this point since that feature was left behind)
* adjusting the token window size for entity-inverted search

To run the indexing process, run this manager
This will annotate the docs in either LOCALDOCS or HUAWEIDOCS and output them to the indexdir folder


## Code Description

### Overview

NERDocumentAnnotator was split off from NERLucene, which was partially developed over the summer of 2017. Where NERLucene generates annotated documents which can be read into a Lucene index and can index those documents, NERDocumentAnnotator only handles the annotation step. It is different in that it supports creating output which can be uploaded directly to Elastic Search, as well as output which can be passed to NERLucene. It contains an annotator for Huawei's dataset, which NERLucene does not. Use of NERDocumentAnnotator is preferred over the annotation step of NERLucene, as it is more complete.

### Manager

In the default package, Manager.java is the project's entry point. The manager has several private class variables which adjust the annotation options. These include

* ANNOTATION_OUTPUT_FORMAT
    Which controls the encoding of annotated documents to be read into a Lucene index. This option is only relevant when making documents for the Lucene plugin.
    
* SPLIT_INDEX
    Which controls whether Entity abstract documents have a _type assigned to them. They do not have a type if an index is created for each entity type. This option has since been disregarded, and should be left false.
    
* NUM_THREADS
    Controls the number of threads used to annotate documents. Please set it to 1.
    
* NUM_DOCS
    The number of documents to annotate from the source.
    
* INSTANCE_TERM_POSITION
    In an E-Inverted document, the defined entity for each abstract document is defined to be at this term number.
    
* ANNOTATION_WINDOW_SIZE
    The number of annotations recorded on each side of the defined entity for an abstract document. Must be <= INSTANCE_TERM_POSITION
    
* DOC_TYPE
    "D" for document-inverted syntax, "E" for entity-inverted syntax.
    
* USE_ES
    Whether the output documents are designed for input into Elastic Search or into the Lucene plugin.
    
* USE_HUAWEI
    Whether a Huawei-provided input document will be used.

### A Note about Huawei input

The option for Huawei input assumes that the input will be in a single file, with each "document" on a new line. The annotator will address this by first splitting the input file into as many thrads as there are, then assigning each thread to its file. For this to work, the annotator used must be the "HuaweiDictionaryExtractor", which uses the "ner/annotation/extraction/huawei_tagging/ChineseQuestionTagging.java" annotatior, which was developed by Denghao Ma. This extractor can be specified in AnnotationConfig.txt, described later

### src/ingestion

The ingestion package is responsible for controlling the annotation threads, called "Ingeser"s. The IngestionManager coordinates these Ingesters for an E-Inverted index, and a WholeDocumentIngestionManager (subclass of IngestionManager) coordinates them for a D-Inverted index.

### src/ner/annotation

The annotation package handles the annotation of documents. It contains an important configuration document, AnnotationConfig.txt

#### AnntationConfig.txt

AnnotationConfig.txt is used to select the NER techniques used to tag the documents.

The first option is to choose the GazetteerStructure, which is to choose an implementation for a dictionary-based approach. GazetteerTree is the best for large numbers of entities, and is the most developed. The others are carryovers for testing purposes from Summer 2017. 

The second option is to choose an EntityOntology. The specified ontology JSON file should reside in src/entity_ontology. This ontology represents the structure of allowed entity types to be tagged. The specification is in JSON, and takes the format
{"EntityType0": ["Subtype0", "Subtype1"], "Subtype0": ["Subsubtype0, ...], ...}

If you are using a gazetteer, the third option is to specify which entity dictionary to use. These can be specified by individual file, or by a folder containing several files. These files/folders should reside in src/entity_instances. The specification is in JSON, and takes the format:
{"EntityType0": [["EntityInstance0Synonym0", "EntityInstance0Synonym1"], ["EntityInstance1Syonym0",...]...], "EntityType1": ...}

The next option is to choose the NER methods themselves. These must be one or more of the existing "EntityExtractor" classes, which wrap a NER technique.

The final option is to choose an AnnotationReconciler, which determines how multiple NER techniques will reconcile their respective annotations. It can also determine whether to index a tagged entity as one or more of its super classes.

### Annotation details

The annotation package is largely the same as the Summer 2017 iteration - its description is copied here.

Overview:

* The Anotation Config file is [NERLucene/src/AnnotationConfig.txt](NERLucene/src/AnnotationConfig.txt)
* AnnotationManager creates NERAnnotators (one per thread)
* NERAnnotator handles NER techniques for a thread.
* EntityCatalogstores all EntityTypes and holds annotations for a thread.
* EntityType describes an entity type like PERSON or PLACE.
* EntityAnnotation is simply a token that may or may not be an entity.
* AnnotationReconciler's subclasses determine how all EntityAnnotaitons from different techniques are put together.
* RootsOnlyReconciler is an EntityAnnotation reconciliation technique implementation.
* SupertypeRootReconciler is an EntityAnnotation reconciliation technique implementation.
* EntityInstance represents a specific entity for a gazetteer method.
* GazetteerTable is a technique for storing a Gazetteer.
* The treegazetteer package contains multiple files for another gazetteer storage technique.
* The extraction package holds implementations of NER techniques.

Detail:

The [AnnotationManager](NERLucene/src/ner/annotation/AnnotationManager.java) creates [NERAnnotator](NERLucene/src/ner/annotation/NERAnnotator.java)s, which will be passed to each thread. Each NERAnnotator has a few important components:

* An [EntityCatalog](NERLucene/src/ner/annotation/EntityCatalog.java)
* An ArrayList of [EntityExtractor](NERLucene/src/ner/annotaiton/extraction/EntityExtractor.java)s
* An [AnnotationReconciler](NERLucene/src/ner/annotation/AnnotationReconciler.java)

#### EntityCatalog

An EntityCatalog contains the following:

##### EntityTypes

The AnnotationManager holds a template EntityCatalog, which is cloned for each NERAnnotator. The EntityCatalog holds the definition of all [EntityType](NERLucene/src/ner/annotation/EntityType.java)s (e.g. #Person, #Place, #Organisation). This EntityType class is designed for a hierarchy of entities (e.g. a #Painter is an #Artist is a #Person) - so each EntityType contains an ArrayList of supertypes.

##### Gazetteer

As dictionary lookup ([Gazetteer](http://www.datacommunitydc.org/blog/2013/04/a-survey-of-stochastic-and-gazetteer-based-approaches-for-named-entity-recognition-part-2)) can be a powerful NER technique, an EntityCatalog uses a [GazetteerTable](NERLucene/src/ner/annotation/GazetteerTable.java). This GazetteerTable holds all defined entities, known as [EntityInstance](NERLucene/src/ner/annotation/EntityInstance.java)s in a special data structure.
Each EntityInstance represents one particular entity (e.g. The "United States" which is a #Country). As an entity instance can have multiple names (e.g. U.S.A., America, United States), an EntityInstance defines several synonyms. The GazetteerTable leverages these synonyms for its data structure. It has a HashMap<EntityType, HashMap<String, ArrayList<EntityInstance>>. The first key is simply to divide by each entity category, and the second key is for each synonym of an EntityInstance. As multiple EntityInstances can potentially have the same name, this maps to an ArrayList of EntityInstances.
As the system is designed, the AnnotaitonManager holds one GazetteerTable which is shared between all EntityCatalogs.

The AnnotationManager can add EntityInstances to the EntityCatalog's Gazetteer in a couple of ways. It can do so by manually creating each EntityInstance, or it can read from a formatted JSON file (addGazetteerEntitiesFromJSON). The JSON must be formatted as:

{EntityType0(ID String) : [EntityInstance0, EntityInstance1, ... EntityInstanceM], EntityType1 : [...], ... EntityTypeN : [...]}

Where each EntityInstance is

[Synonym0, Synonym1, ..., SynonymK]

There are three optional Gazetteer structures for the EntityCatalog, it is recommended you use only one.

* GazetteerTree
* GazetteerTable
* GazetteerList (just an ArrayList<EntityInstance>).

In the AnnotationManager's setUpCatalog function, comment in the one you wish to use. Note that the GazetteerTree is paired with the [GazetteerTreeExtractor](NERLucene/src/ner/annotation/extraction/GazetteerTreeExtractor.java),
The GazetteerTable is paired with the [GazetteerTableExtractor](GazetteerTreeExtractor](NERLucene/src/ner/annotation/extraction/GazetteerTableExtractor.java), and the GazetteerList is paired with the [StanfordGazetteerExtractor](NERLucene/src/ner/annotation/extraction/StanfordGazetteerExtractor.java)

The StanfordGazetteerExtractor also requires the use of a [StanfordGazetteerPatternBuilder](NERLucene/src/ner/annotation/extraction/StanfordGazetteerPatternBuilder.java), which builds RegEx from the EntityInstances. If you wish to use the StanfordGazetteerExtractor, comment lines with the patternBuilder in.

##### Annotaiton Array

Each EntityCatalog contains an array of keyword and entity tokens. Each of these tokens is known as an [EntityAnnotation](NERLucene/src/ner/annotation/EntityAnnotation.java). The NERAnnotator uses this array of tokens to build the annotation for each document, and clears it between documents.
Each EntityAnnotation contains some important indexing information such as start offset and the content of the token. In addition to this, each EntityAnnotation can represent multiple EntityTypes (e.g. an entity that is both a #Painter and a #Sculptor). Also, during the annotation process it's possible that different NER techniques label overlapping tokens.
For example, one technique may label "The University of Illinois at Urbana-Champaign" as an #Organisation, while another may label "Illinois" as a #State, and "Urbana-Champaign" as a #City. To record all of these overlapping annotations, each EntityAnnotation contains any generated EntityAnnotaitons which are substrings of itself, forming a tree. It is up to the EntityCatalog to add new EntityAnnotations to the correct place in the tree.

#### EntityExtractor

An [EntityExtractor](NERLucene/src/ner/annotation/extraction) represents a NER technique. An example of this is [Stanford NER](https://nlp.stanford.edu/software/CRF-NER.shtml). A subclass of an EntityExtractor takes text as input, and adds EntityAnnotations to an EntityCatalog.
As each EntityExtractor has access to the catalog, it is possible to use previously tagged EntityAnnotations as features, or for any other purpose. A NERAnnotator defines which EntityExtractors to use, and in what order. Note: any non-entity keywords that are not added to the EntityCatalog's annotation array will not be recorded, so it is likely beneficial to implement a tokenization method as an EntityExtractor.

#### TreeGazetteer

in the [treegazetteer package](NERLucene/src/ner/annotaiotn/treegazetteer/) there is an implementation of a prefixtree for entity string matching.
It also contains an optional Stanford Part of Speech tagger to limit the matched strings to noun phrases. See the analysis [here](https://docs.google.com/presentation/d/1gHIeSFnboyou66svHO71gmzN2PGEGfPvwSUqw3eXYHY/edit#slide=id.g2445212d58_0_38).

The [GazetteerTree](NERLucene/src/ner/annotaiotn/treegazetteer/GazetteerTree.java) contains a tree of [GazetteerNode](NERLucene/src/ner/annotaiotn/treegazetteer/GazetteerNode.java)s, with a character marking each transition between nodes.
If the characters required to traverse the tree from the root a node form a synonym of an EntityInstance, that node contains that instance. In this way, the [GazetteerTraverser](NERLucene/src/ner/annotaiotn/treegazetteer/GazetteerTraverser.java) can perform pattern matching.

The associated EntityExtractor, [GazetteerTreeExtractor](NERLucene/src/ner/annotation/extraction/GazetteerTreeExtractor.java) has a character for delimiting tokens. It reads the input in char by char, and when a delimiter is seen, a new GazetteerTraverser is spawned. When a GazetteerTraverser
makes a transition that does not lead to a node, it is removed from the extractor. When the node find EntityInstances in its current node, the GazetteerTreeExtractor adds it to its annotations.

#### AnnotationReconciler

Because multiple NER techniques can be used, there should be a way to determine how to handle overlapping or conflicting annotations. It is the job of an [AnnotationReconciler](NERLucene/src/ner/annotation/extraction/AnnotationReconciler.java) to determine which annotations to keep, and whether any type of merging should occur.

## Authors

* **Alex Aulabaugh** - aaulabaugh@gmail.com
* **Denghao Ma** - *ner/extraction/huawei_tagging*

Credit to:

* **Jaewoo Kim** - *Indexing* - jkim475@illinois.edu

* **Jintao Jiang** - *Querying, Ranking* - jjang43@illinois.edu

For their work over the Summer iteration