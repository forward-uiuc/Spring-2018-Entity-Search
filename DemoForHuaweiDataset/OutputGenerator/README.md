# Generate text file for WebInterface
Thit Python script is used for splitting the entire Question and Answer input file into seperate text files. Each text file contains a question and its corresponding answer, and it is used in the webInterface when user clicks on read more to displaying the original text. 

## Set up input and output path
 - Line 3 fJson points to the json file generated from the annotation and indexing process. In our case, it should locate at: 
```
 path/to/project/DemoForHuaweiDataset/2018sp-entitylucene/NERDocumentAnnotator/src/indexdir/ESDocs/esdata0.json
```

 - Line 4 fAnno points to the annotation result which contains the orginal question and answer and annotated question. In our case, it should locate at: 
```
 path/to/project/DemoForHuaweiDataset/EntitySearch/dictionaries/entitySearchLabelResults
```

 - Line 32 fOut points to the output folder contains seperate text file. In out case, it should be: 
```
 path/to/project/DemoForHuaweiDataset/EntityQAWebInterface/backend/Output
```

## Run the script
In terminal:
```
 cd path/to/project/DemoForHuaweiDataset/OutputGenerator
 python3 FileMatch.py
```