# Lucene Entity Search

This project provides a framework for building an entity-enabled Lucene searchable index.

As NERLucene remains quite similar to the version developed over the summer iteration, that readme has been copied here. The notable difference is in the annotation protion can either put the annotated
documents into a Lucene index, or write them out to serialized annotated documents. It can then later read these serialized documents into the index. This is controlled in LOCALPATHS.txt

Set "LOCALDOCTYPE:" to "plaintext" if the input documents are plain text.

Set "LOCALDOCTYPE:" to "lucene" if the input documents are serialized annotated documents.

Set "OUTPUT:" to "index,y" where y is the location to store the index, or to "lucene,y" where y is the location to store the annotated docs.

## Getting Started

These instructions will get you a copy of the project up and running on your local machine for development and testing purposes.

### Installing

####Cloning
The following instructions are for setting up Lucene Entity Search in Eclipse

First, clone the repo

```
git clone https://username@bitbucket.org/forward-uiuc/2017f-entitylucene.git
```

In Eclipse, use File > Open Projects From Filesystem...
Import NERLucene and lucene-solr-master/lucene.

####Build Paths
Next you have to configure the build paths.

NERLucene

* Begin by rightclicking NERLucene in the Package Explorer
    * Build Path > Configure Build Path OR Properties > Java Build Path
* In the Projects tab add lucene
* In the Libraries tab, remove any jars not in the JRE System Library
* Click Add External Jars...
    * Add all jars in the NERLuceneDependencies folder
* Click Apply
 
Lucene

* Begin by rightclicking lucene in the Package Explorer
    * Build Path > Configure Build Path OR Properties > Java Build Path
* In the Projects tab, remove everything
* In the Libraries tab, remove any jars not in the JRE System Library
* Click Add External Jars...
    * Add all jars in the LuceneDependencies folder (extract from LuceneDependencies.zip
* Click Apply

####Datasets

The datasets used while developing this project are located on harrier01.cs.illinois.edu:/scratch/DatasetArchives/Archives-Semesters/2017M/2017M-ERSearch

There are Four zip files:

* EntityInstancesDBPedia.zip
Contains all entity instances from DBPedia as csv files, as well as the python script to parse them into a JSON file, and the output JSON. The output JSON is already included in
bitbucket [here](/NERLucene/src/entity_instances), but the full original dataset is on harrier01 for completeness.
* OntologyDBPedia.zip
Contains the method for extracting an ontology file from DBPedia and associated files. As with the entity instances, there is a copy of the output in bitbucket [here](/NERLucene/src/entity_ontology).
* InputDocuments.zip
Contains the test datasets for documents to be indexed. The largest (and best) of which is in the imdb folder. The dataset is a series of plot summaries for movies and TV shows listed on IMDB, which can be found [here](http://ftp.sunet.se/mirror/archive/ftp.sunet.se/pub/tv+movies/imdb/).
plot.list.gz and plotParser.py were used to expand this dataset into one file per plot. It is not recommended that the imdb folder be imported into Eclipse's Package Explorer, as this can cause a crash.
* StanfordNER_Training_Ground.zip
Contains the files needed to train a Stanford CRF Classifier with a gazetteer turned on.

####Demo

To configure the demo, open [LOCALPATHS.txt](/LOCALPATHS.txt)

Note that the paths used her must be absolute paths.


* Edit the DINVSCHEMA line to "DINVSCHEMA:/path/to/project/NERLucene/src/formatting/DInvPostingsFormat.xml"
    * This is the [PostingsFormatter](/NERLucene/src/formatting/PostingsFormatter.java) schema file for a d-inverted index
* Edit the EINVSCHEMA line to "EINVSCHEMA:/path/to/project/NERLucene/src/formatting/EInvPostingsFormat.xml"
    * This is the [PostingsFormatter](/NERLucene/src/formatting/PostingsFormatter.java) schema file for an e-inverted index
* Edit the LOCALDOCS line to "LOCALDOCS:/path/to/project/NERLucene/src/docs"
    * This is the directory with textfiles to be indexed. You may use your own, or a dataset from Harrier01 menioned above.
* Edit the CLASSIFIER line to "CLASSIFIER:/path/to/project/NERLuceneDependencies/english.all.3class.distsim.crf.ser.gz"
    * This is the location of the StanfordNER classifier
* Edit the POSTAGGER line to "POSTAGGER:/path/to/project/NERLuceneDependencies/english-bidirectional-distsim.tagger
    * This is the location of the Stanford Part of Speech tagger.

The default values in [NERLucene/src/AnnotationConfig.txt](NERLucene/src/AnnotationConfig.txt) dictate we use a StanfordNER three class CRFClassifier, and we work with an EntityOntology from DBPedia.

Use the imdb dataset from Harrier01 for predictable results. That means setting the LOCALDOCS line in LOCALPATHS.txt to that folder.

Open [NERLucene/src/IngestionManager.java](NERLucene/src/IngestionManager.java) and ensure the class variables NUM_THREADS is 1, NUM_DOCS is 100, ANNOTATION_OUTPUT_FORMAT is "encodedString", and INDEXING_OUTPUT_FORMAT is "encodedString".
NOTE: JSON deserialization for searching using [GSON](https://github.com/google/gson) is currently bugged, use base64encoding to enable searching. The bug occurs at [NERLucene/src/formatting/TokenContent.java](NERLucene/src/formatting/TokenContent.java) at line 195.
To manually inspect the index, set USE_SIMPLE_TEXT_CODEX to true and INDEXING_OUTPUT_FORMAT to "JSON". This is makes a human readable output.

To run the indexing process, run [/NERLucene/src/IngestionManager.java](/NERLucene/src/IngestionManager.java).
This will index the docs in LOCALDOCS to the NERLucene/src/indexdirD (d-inverted index) and NERLucene/src/indexdirE (e-inverted index) folders.
This process should take roughly 1.5 seconds and will yield the output
```
ADDING JSON GAZETTEER ENTRIES...DONE - ADDED 1117160 INSTANCES WITH 1259908 IDENTIFIERS
Loading classifier from /absolute/path/2017m-ersearch/NERLuceneDependencies/english.all.3class.distsim.crf.ser.gz ... done [1.1 sec].
THREAD 0 ASSIGNED TO DOCS [0, 100)

BEGIN INDEXING
StanfordExtractor: FOUND 586 ENTITIES
THREAD FINISHED IN 1.384 SEC

INDEXED 100 DOCS WITH 1 THREAD(S) IN: 1.558 Seconds

Running query: 'entity_place'
Found 10 hits.
...
```
To run a sample query, Navigate to [NERLucene/src/TestQuery.java](NERLucene/src/TestQuery.java) and run it. This searches the query "[(is | cool) #person]<10>". The result should be:
```
Anonymous ---- 5.32619
Crabtree ---- 0.2
```
And should complete nearly instantaneously.

## Code Workflow

The general idea of this project is to first annotate and load [Named Entities](https://en.wikipedia.org/wiki/Named_entity) from documents into an [Inverted Index](https://en.wikipedia.org/wiki/Inverted_index) using [Apache Lucene](https://lucene.apache.org/core/), then to extend Lucene's index searching to handle queries to return Named Entities.
This project Lucene's existing classes where it could, and provides some external structures for elements not natural to Lucene. It also supports basic usage from the command line, as well as basic parallelization of the indexing process.
For background in Lucene, [Manning Lucene In Action 2nd Edition](https://www.manning.com/books/lucene-in-action-second-edition) is a recommended read.

### [Default Package](NERLucene/src/)

* [IngestionManager](NERLucene/src/IngestionManager.java) sets up NERAnnotation and indexing threads. It contains the main function for building an index.
* [TestQuery](NERLucene/src/TestQuery.java) contains the main function for searching the index for entity results.
* [CommandLineConsole](NERLucene/src/CommandLineConsole.java) contains a demo implementation of a command line tool to control indexing and searching.
* [AnnotationConfig](NERLucene/src/AnnotationConfig.txt) contains specifications for NER.

#### Multithreading Detail

The NER Annotation and indexing process begins with the [IngestionManager](NERLucene/src/IngestionManager.java). This file is responsible for creating and joining all annotating & indexing threads (called [Ingester](NERLucene/src/indexing/Ingester.java)s). The IngestionManager also creates an [AnnotationManager](NERLucene/src/ner/annotation/AnnotationManager.java), which is used to assign an [NERAnnotator](NERLucene/src/ner/annotation/NERAnnotator.java) to each Ingester.
The IngestionManager also creates an [IndexingManager](NERLucene/src/indexing/IndexingManager.java). This IndexingManager controls the index itself. Each Ingester will call addDocsFromDirectory in the IndexingManager, passing in the range of documents to add and their specified NERAnnotator. This function will read the file from disk, use the NERAnnotator to annotate the contents, create a lucene document, and index it. Lucene's [IndexWriter](lucene-solr-master/lucene/core/org/apache/lucene/index/IndexWriter.java) is thread safe, so an IndexingManager will reuse them between threads.

### [NER Annotation](NERLucene/src/ner/annotation/)

Named Entity Recognition ([NER](https://en.wikipedia.org/wiki/Named-entity_recognition)) is an ongoing area of research, so this project aims to support many types of NER to be implemented by the user as opposed to commiting to any one strategy.
All NER is performed outside of Lucene. The [NER Annotation package](NERLucene/src/ner/annotation) handles this process. The end goal of the NER Annotation phase of the indexing process is to produce a formatted token list with keywords and entities for each document. These formatted lists are then sent into Lucene's indexing process as fields in Lucene documents, where their content is interpereted.

Overview:

* The Anotation Config file is [NERLucene/src/AnnotationConfig.txt](NERLucene/src/AnnotationConfig.txt)
* [AnnotationManager](NERLucene/src/ner/annotation/AnnotationManager.java) creates NERAnnotators (one per thread)
* [AnnotationManager_cmd](NERLucene/src/ner/annotation/AnnotationManager_cmd.java) sets up an annotation manager with the command line.
* [NERAnnotator](NERLucene/src/ner/annotation/NERAnnotator.java) handles NER techniques for a thread.
* [EntityCatalog](NERLucene/src/ner/annotation/EntityCatalog.java) stores all EntityTypes and holds annotations for a thread.
* [EntityType](NERLucene/src/ner/annotation/EntityType.java) describes an entity type like PERSON or PLACE.
* [EntityAnnotation](NERLucene/src/ner/annotation/EntityAnnotaiton.java) is simply a token that may or may not be an entity.
* [AnnotationReconciler](NERLucene/src/ner/annotation/AnnotationReconciler.java)'s subclasses determine how all EntityAnnotaitons from different techniques are put together.
* [RootsOnlyReconciler](NERLucene/src/ner/annotation/RootsOnlyReconciler.java) is an EntityAnnotation reconciliation technique implementation.
* [EntityInstance](NERLucene/src/ner/annotation/EntityInstance.java) represents a specific entity for a gazetteer method.
* [GazetteerTable](NERLucene/src/ner/annotation/GazetteerTable.java) is a technique for storing a Gazetteer.
* The [treegazetteer](NERLucene/src/ner/annotation/treegazetteer/) package contains multiple files for another gazetteer storage technique.
* The [extraction](NERLucene/src/ner/annotation/extraction/) package holds implementations of NER techniques.

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

* [GazetteerTree](NERLucene/src/ner/annotaiotn/treegazetteer/)
* [GazetteerTable](NERLucene/src/ner/annotation/GazetteerTable.java)
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

### [Analysis](NERLucene/src/ner/analysis/)

Overview:

* [NERAnalyzer](NERLucene/src/ner/analysis/NERAnalyzer.java) holds a tokenizer and a token filter to create a token stream from an input document.
* [NERTokenizer](NERLucene/src/ner/analysis/NERTokenizer.java) reads serialized NERAnnotations and converts them to tokens.
* [DInvNERTokenizer](NERLucene/src/ner/analysis/DInvNERTokenizer.java) formats these tokens for a D-Inverted index.
* [EInvNERTokenizer](NERLucene/src/ner/analysis/EInvNERTokenizer.java) formats these tokens for an E-Inverted index.

Detail:

In Lucene, an Analyzer uses a Tokenizer and any number of TokenFilters to create a token stream for a document. For NERLucene, a [NERTokenizer](NERLucene/ner/analysis/NERTokenizer.java) is meant to parse the output of the NER annotation process into tokens, then use a [PostingsFormatter](NERLucene/src/formatting/PostingsFormatter.java) to record token information (including entity information) to the index.
If any EntityAnnotations contain sub annotations, all variations will be added to the index with the same term number. Additionally, an EntityAnnotation with multiple EntityTypes will be added once to the index per type, again with the same term num. The same applies for any EntityTypes with parent types.
For example, referencing dbpedia's [entity ontology](http://web.informatik.uni-mannheim.de/DBpediaAsTables/DBpedia-en-2016-04/DBpediaClasses.htm), an EntityAnnotation which has EntityTypes #Singer and #Boxer would be index with the following types: #Singer, #MusicalArtist, #Artist, #Person, #Agent, #owl:Thing, #Boxer, #Athlete.
To enable multithreading, the [NERAnalyzer](NERLucene/src/ner/analysis/NERAnalyzer.java) and NERTokenizers are thread safe. 

### [Indexing](NERLucene/src/indexing) & [Formatting](NERLucene/src/formatting)

Indexing Overview:

* [Ingester](NERLucene/src/indexing/Ingester.java)s represent one thread for NER annotation and indexing.
* [IndexingManager](NERLucene/src/indexing/IndexingManager.java) is used by the Ingesters to write to the index.
* [TextFileFilter.java](NERLucene/src/indexing/TextFileFilter.java) filters the input documents to be only .txt.

Formatting Overview:

* [TokenContent](NERLucene/src/formatting/TokenContent.java) The token content is the internal hashmap that saves the postings field data before it is flushed into the actual lucene postings
* [PostingsFormatter](NERLucene/src/formatting/PostingsFormatter.java)The postings formatter can be implemented for formatters that format input from analyzers in to postings. The formatter reads a schema file to define structure of the token content class. Creates a TokenContent.
* [TokenField](NERLucene/src/formatting/TokenField.java)define a data type that can be handled by the postings.

Detail:

Broadly, indexing is a technique for preprocessing data such as location about documents so that search of those objects is fast at runtime. For more information visit [link](https://en.wikipedia.org/wiki/Search_engine_indexing). Lucene's original index enables fast document search from a keyword. Our system enhances the original index to not only search for documents, but entities, which are at a more specific granularity than entities. 

All of classes described in this section are contained in the [Formatting](NERLucene/src/formatting/) package. 

Generally, after values for the postings of an index are generated by the Analysis stage, these postings values have to be formatted so they can be inputted in to our new Entity Aware Lucene Index. As mentioned in this [paper](http://forward.cs.illinois.edu/pubs/2010/entityindex-edbt2010-cc-jan2010.pdf), an entity aware index requires more information to be added to the postings than the original keyword index. The original Lucene Index provides [payloads](https://lucene.apache.org/core/4_0_0/core/org/apache/lucene/analysis/tokenattributes/PayloadAttribute.html) for each tokens in the index. These payloads are arbitrary byte arrays that can be used to hold data. We took advantage of these payloads to store custom postings values. In order to add data to the payloads, the data has to be serialized to a byte array. So the indexing formatter provides and interface for serializing and deserilizing postings data without the user needing to explicitly set payloads. Later in the querying stage, these entity aware postings can be read again by deserilizing the payload content. All of this process is done through an interface, so the user does not need to actually care about bytel-evel payloads. 

To elaborate on this interface, several important java classes must be mentioned. Most importantly, Implementations of the PostingsFormatter such as the [DinvFormatter](NERLucene/src/formatting/DInvFormatter.java) and [EinvFormatter](NERLucene/src/formatting/EInvFormatter.java) map values generated from the analyzer to different [lucene attributes](https://lucene.apache.org/core/6_5_1/core/org/apache/lucene/util/Attribute.html). Out of many lucene attributes, the payload attribute is one of them. These lucene attributes can be thought of as information that will be added to the postings. In the original workflow of Lucene, all lucene attributes are manually set for all tokens in the [incrementToken()](NERLucene/src/ner/analysis/NERTokenizer.java) function, but we abstracted the process in the PostingsFormatter with the TokenContent class.  All lucene attributes including payloads are set with data generated from the custom analyzer through the [TokenContent](NERLucene/src/formatting/TokenContent.java), and the action of setting all the attributes is done with commit function of the PostingsFormatter. 

Different types of indexes can also be set up. Each formatter needs to know what kind of index is being created, and we wanted the system to be general so that any type of index can be created, so long as input data can be analyzed. So PostingsFormatter reads a xml schema file to define what contents will be added to the postings(payloads). These schema files are read by a [PostingSchemaReader](NERLucene/src/formatting/PostingSchemaReader.java) to create TokenFields. The [TokenField](NERLucene/src/formatting/TokenField.java) is an interface that can be implemented for defining atomic types of data to be entered into the postings. Currently int,float,text,date,context types of data are supported. They are datatypes that will be serialized according to their TokenField into the index. 

The deserilization process much more simple. In the original querying workflow, the [postingsEnum](https://lucene.apache.org/core/5_2_0/core/org/apache/lucene/index/PostingsEnum.html) is used to access lucene attriutes. We abstracted this access through the PostingsFormatter. In our system, with the input of the postingsEnum, the deserialize function of PostingsFormatter will return a list of [Entities](NERLucene/src/formatting/Entity.java) which represent all the payload values that were serialized into the index. 

As an aside, this serialization/deserialization can be done with JSON or base64encoding of the byte array returned by calling serialize on each TokenField. 

NOTE: JSON deserialization for searching using [GSON](https://github.com/google/gson) is currently bugged, use base64encoding to enable searching.

### [Searching](NERLucene/src/query/)

####Big picture
The search module lies in packages "pattern" and "query". 

The "query" package provides users with the ability to search into a document, or in other word, search entities. This package contains classes that extend Lucene's classes for searching and strictly follows that workflow. We'll elaborate this package later. 

The "pattern" package constructs the query language for this system which we named it CQL. With CQL, users don't need to touch the underlying classes but only need to do queries just like how they query datatbase using SQL. However, the package is still under construction. Instead, what we provide right now is called pattern searching. There are two patterns for users to use. 

First of all, the window pattern, eg. {uiuc cs #person}. This means uiuc, cs AND entity person should appear in the same document. 

Secondly, the disjunction pattern, eg. [(uiuc | cs) #person]<10>. This means uiuc OR cs should appear within 10 words window of entity person. 

For both pattern users can only search with one entity and multiple keywords.

####Example
There is an example usage in [TestQuery.java](NERLucene/src/TestQuery.java). Before using this file, you need to have document indexed already. This file is calling an object called [PatternQueryHandler](NERLucene/src/query/PatternQueryHandler.java) which is the entrance of the search module. The PatternQueryHandler takes in as input the Pattern, IndexReader, IndexSearcher and the mode you chose to create the index. When you initialize the object, it uses the lexer and parser generated by [PatternFlex.jflex](NERLucene/src/pattern/PatternFlex.jflex) and [PatternCup.cup](NERLucene/src/pattern/PatternCup.cup) to parse the pattern to an object called Token. Then when you call the function execute(), it first decides whether the token is a SeqToken or a SetToken. SeqToken is for window pattern while SetToken is for disjunction pattern. Then it calls [EntityIndexSearcher](NERLucene/src/query/EntityIndexSearcher.java) to collect and score the entities and [Aggregator](NERLucene/src/query/Aggregator.java) to aggregate the results.

####Workflow of Lucene
This is to make you familiar with Lucene's searching workflow and prepare you for the next few sections. You can jump between files through IDE. The "IndexSearcher" needs two things: "IndexReader" and "Query". Every Lucene Query consists of three things: Query, Weight and Scorer. I'm only using "TermQuery" so I'll take this as an example. When you create "TermQuery", it also creates a "TermWeight" which holds the "Posting" of a single term. Then "TermWeight" will create "TermScorer" and passes only the frequency attribute of the posting to the "TermScorer". Weight classes will call scoreAll() to iterate through the documents and pass a single document to "TopScoreDocCollector". The collect() function in "TopScoreDocCollector" will use the Scorer to score the document and collect the document into "TopDocs". The "TopDocs" is maintaining a priority queue called "HitQueue". The priority queue has a fixed size which is specified by users. So Each time a document is scored, it overrides the top node of the queue and then the queue is resorted. "TopDocs" will call reduce() to merge results from different shards. However, the shards number is 1 by default. Finally the "IndexSearcher" will return the "TopDocs" to users. Some asides here: the Scorer is using Similarity classes to score the document. The default Similarity class is "BM25Similarity" but you can set the class using setSimilarity() in "IndexSearcher".

####Implementation
#####Collect entities
I extended some Lucene classes to make entity search possible. The package is built on E-Inverted Index and "TermQuery".

["EntityWeight.java"](NERLucene/src/query/EntityTermQuery.java): This is an inner class of "EntityTermQuery". It passes all the attributes including Payload to the Scorer instead of only frequency attribute.

["EntityScorer.java"](NERLucene/src/query/EntityScorer.java): Provides functions for collecting and scoring entities. The getEntity() function returns a list of entity inside the current document. It iterates through each position and get the payload under that position. Here, I use JaeWoo's [EInvFormatter](NERLucene/src/formatting/EInvFormatter.java) to deserialize the payload. The score() function calls [EntitySimilarity](NERLucene/src/query/EntitySimilarity.java) to score the entity.

["TopEntityCollector.java"](NERLucene/src/query/TopEntityCollector.java): Entities are collected and scored here. Calls getEntity() to get the entity list and checks if the entity type matches the input type and if the entity is within the window of the keyword. If so, scores it and makes an [EntityScoreDoc](NERLucene/src/query/EntityScoreDoc.java) for it and update the [EntityHitQueue](NERLucene/src/query/EntityHitQueue.java).

#####Score entities
["EntitySimilarity.java"](NERLucene/src/query/EntitySimilarity.java): Scores entity based on the distance between it and the keyword. This produce the local score.

["Aggregator.java"](NERLucene/src/query/Aggregator.java): Iterate through the TopDocs and accumulate the scores of entities with same entity values. This produce the global score. It also sorts the results by their scores.

####Pattern search
See [PatternQueryHandler](NERLucene/src/pattern/PatternQueryHandler.java).

Disjunction pattern: This is simpler than window pattern. For each keyword, a TopDocs will collect qualified entities around it. Since keywords are disjunctive, Aggregator will simply accepts all of them. 

Window pattern: Since the keywords and entity should appear in the same document, we should check this condition before collecting the entities. I tried to use BooleanQuery but it refused my Scorer because there were no function called getEntity() in the super class. Unless I extend BooleanQuery, I cannot take advantage of it. And I found out that I needed to extend every Lucene's Query class before using them and this is too troublesome. Finally, I chose to avoid the problem and decided to check the condition after entities were collected. I first used BooleanQuery to get the list of intersected documents and EntityTermQuery to get the list of entities and then erased entities not coming from these documents in the Aggregator. This is three-pass which is a little slow and awkward. This problem will be reiterated in the latter section.

####Pattern parse
["PatternFlex.jflex"](NERLucene/src/pattern/PatternFlex.jflex): Lexer that breaks a string into tokens.

["PatternCup.cup"](NERLucene/src/pattern/PatternCup.cup): Parser that takes some action according to the pattern tokens display.

These two files parse the user input and store information into either SetToken or SeqToken. They are to Mianwei's credit since he's the author of CQL. However, since the structures of our systems are different, other files inside pattern package are different and the parse tree is not translated to an execution tree either.

####Difference between NERLucene and Docqs
This is a further discussion about why the pattern package is different. All the discussion is based on the code lies in "https://bitbucket.org/forward-uiuc/zhou18-entity-search". Mianwei didn't use Lucene's searching functionalities so he could do anything freely. The Token is translated to RegNode when the parse tree is converted to execution tree. The RegNode offers two functions. NextDoc() uses keywords to find the document id from Inverted Index, and getDataInfoList() uses the document id to get entities from Forward Index. Then he sents the entities to Fang Yuan's system to have them scored. These two functions are executed by leaf nodes because they are independent. The keyword node is used to search document, and the entity node is used to retrieve entities. But in order to integrate with Lucene, I need to send both keyword and entity information to "EntityTermQuery" so it's not possible to use an execution tree. Instead, I only use the information stored in the Token. That's why the code is different. However, when later CQL is implemented, it's natural to have some form of execution tree.

####Problem with current system
As I mentioned in the implementation of window pattern, whenever I need to use a Lucene's query I need to extend it which is a lot of work and inelegant. This is because I insert two functions, getEntity() and score(int pos, int epos), into "EntityScorer" which is not accepted by the super class. So I convert the class type in the Collector but when I want to use other scorers they are not convertible. I did so because I thought entities can only be collected and scored inside Scorer. 

However, now I think it's possible to collect and score entities elsewhere. For collecting, you can pass the Posting to the Collector at the same time as you pass the Scorer and then get the entities according to the doc id. See [TopEntityCollector](NERLucene/src/query/TopEntityCollector.java). For scoring, you need to figure out a way to pass the Scorer the information about the current entity. I suggest analyzing [PayloadScoreQuery](lucene-solr-master/lucene/queries/src/java/org/apache/lucene/queries/payloads/PayloadScoreQuery.java) to see how they deal with payload. Anyway, I believe there should exist an elegant way to fit entity-search better into Lucene.

I once proposed to change the index structure. I wanted to take entity as document and index them directly, and thought the collection and scoring would be directly taken care of by Lucene. That idea seems immature and just offer here as a potential solution. After all, these are just my guesses and they need experiement. Nevertheless, one thing I believe is that it's not a good practice to extend TermQuery. We should build our own general query so that it can use all of Lucene's Query easily. In order to do that, developers should gain very deep understanding of Lucene's source code.

####TODO
1. Implement CQL. There should be a rewrite system and an execution tree.
2. Refine the query module to make it compatible with all the Lucene's Query classes. Also think about supporting entity-search via different index. The current solution relies on E-Inverted Index which maps keyword to nearby entity. Try to use D-Invert Index which maps entity to document.
3. Improve the performance. Think about pre-sorting and returning results in a stream.

## Built With

* [Lucene Build Instructions (apache ant)](https://svn.apache.org/repos/asf/lucene/dev/branches/lucene539399/lucene/BUILD.txt)

## Authors

* **Jaewoo Kim** - *Indexing* - jkim475@illinois.edu
* **Jintao Jiang** - *Querying, Ranking* - jjang43@illinois.edu
* **Alex Aulabaugh** - *NER, Tokenization* - aaulabaugh@gmail.com

