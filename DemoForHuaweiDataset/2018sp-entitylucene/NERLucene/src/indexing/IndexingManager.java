package indexing;

import java.io.File;
import java.io.FileFilter;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Scanner;

import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.codecs.simpletext.SimpleTextCodec;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.FieldType;
import org.apache.lucene.document.IntPoint;
import org.apache.lucene.document.LongPoint;
import org.apache.lucene.document.StoredField;
import org.apache.lucene.document.StringField;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexOptions;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.index.IndexableField;
import org.apache.lucene.index.MultiReader;
import org.apache.lucene.index.Term;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.PhraseQuery;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TopScoreDocCollector;
import org.apache.lucene.search.WildcardQuery;
import org.apache.lucene.search.spans.FieldMaskingSpanQuery;
import org.apache.lucene.search.spans.SpanMultiTermQueryWrapper;
import org.apache.lucene.search.spans.SpanNearQuery;
import org.apache.lucene.search.spans.SpanQuery;
import org.apache.lucene.search.spans.SpanTermQuery;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.SimpleFSDirectory;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import java.lang.reflect.Type;

import ner.analysis.InstanceInvNERTokenizer;
import ner.analysis.NERAnalyzer;
import ner.annotation.EntityCatalog;
import ner.annotation.NERAnnotator;

/**
 * Interface for writing to the indexes.
 * TODO make configurable (command line?)
 * @author alexaulabaugh
 */

public class IndexingManager
{	
	//The files to index
	protected static File[] filesToIndex;
	
	//Filters filetypes
	protected static FileFilter filter;
				
	protected static String inputFormat;
	
	protected static String outputFormat;
	
	protected static boolean useSimpleText;
	
	protected static int instancePosition;
	
	protected static boolean splitIndex;
	
	protected static String inputDocType;
	
	protected static String outputGoal;
	
	protected static String outputDestination;
	
	protected HashMap<String, IndexWriter> indexWriters;
	
	protected String[] entityPayloadFields;
	protected String[] termPayloadFields;
	
	protected Gson gson;
			
	public IndexingManager(String inFormat, String outFormat, Boolean simpleText, int instPos, Boolean split, String[] entityPayloadFields, String[] termPayloadFields)
	{
		try
		{
			filter = new TextFileFilter();
			//Get local path
			String path = new File(".").getAbsolutePath();
			String[] pathArr = path.split("/");
			path = String.join("/", Arrays.copyOfRange(pathArr, 0, pathArr.length-2));
			String localPathsFile = path + "/LOCALPATHS.txt";
			List<String> lines = Files.readAllLines(Paths.get(localPathsFile));
			String localDocs = "";
			inputDocType = "";
			outputGoal = "";
			outputDestination = "";
			for(int i = 0; i < lines.size(); i++)
			{
				String[] line = lines.get(i).split(":");
				if(line[0].equals("LOCALDOCS"))
					localDocs = line[1];
				else if(line[0].equals("LOCALDOCTYPE"))
					inputDocType = line[1];
				else if(line[0].equals("OUTPUT"))
				{
					String[] outputInfo = line[1].split(",");
					outputGoal = outputInfo[0];
					outputDestination = outputInfo[1];
				}
			}
			filesToIndex = new File(localDocs).listFiles();
			
			inputFormat = inFormat;
			outputFormat = outFormat;
			useSimpleText = simpleText;
			instancePosition = instPos;
			splitIndex = split;
			this.entityPayloadFields = entityPayloadFields;
			this.termPayloadFields = termPayloadFields;
			indexWriters = new HashMap<String, IndexWriter>();	
			gson = new GsonBuilder().create();
		}
		catch(IOException e)
		{
			e.printStackTrace();
		}
	}
	
	/**
	 * Change the files to index
	 * @param pathToFiles within indexdir
	 */
	public void setFilesToIndex(String pathToFiles)
	{
		filesToIndex = new File("src/indexdir/" + pathToFiles).listFiles();
	}
	
	/**
	 * Gets the indexWriter appropriate for the entity type, or creates a new indexWriter/directory
	 * if there is none for this type
	 * @param entityTypeID
	 * @throws IOException
	 * @return
	 */
	private IndexWriter getIndexWriterForType(String entityTypeID) throws IOException
	{
		if(!splitIndex)
			entityTypeID = "all";
		if(!indexWriters.containsKey(entityTypeID))
		{
			System.out.println("ERROR: NO INDEX WRITER FOR ENTITY TYPE: " + entityTypeID);
			return null;
		}
		return indexWriters.get(entityTypeID);
	}
	
	/**
	 * Entry point for adding documents from a directory.
	 * @param start
	 * @param end
	 * @param annotator
	 * @throws Exception
	 */
	public void addDocsFromDirectory(int start, int end, NERAnnotator annotator) throws Exception
	{
		if(inputDocType.equals("plaintext"))
		{
			if(outputGoal.equals("index"))
				addDocsFromTextfileDirectory(start, end, annotator);
			else
				annotateDocsFromTextfileDirectory(start, end, annotator, outputDestination);
		}
		else if(inputDocType.equals("lucene"))
			addDocsFromLucenedocDirectory(start, end);
		else
			throw new Exception("INVALID INPUT DOC TYPE - PLEASE SEE LOCALPATHS.txt");
	}
	
	/**
	 * perform NER on plaintext docs
	 * @param start
	 * @param end
	 * @param annotator
	 * @param destination
	 * @throws IOException
	 */
	public void annotateDocsFromTextfileDirectory(int start, int end, NERAnnotator annotator, String destination) throws IOException
	{
		String destinationPath = "src/indexdir/" + destination;
		File destinationDirectory = new File(destinationPath);
		//https://stackoverflow.com/questions/3775694/deleting-folder-from-java
		if(destinationDirectory.exists())
		{
			File[] subfiles = destinationDirectory.listFiles();
			if(subfiles != null)
			{
				for(File subfile : subfiles)
					subfile.delete();
			}
			destinationDirectory.delete();
		}
		destinationDirectory.mkdir();
		// CITATION:
		// https://www.tutorialspoint.com/lucene/lucene_indexing_process.htm
		for (int docnum = start; docnum < end; docnum++)
		{
			File file = filesToIndex[docnum];
			// if the file is valid
			if (file.isDirectory() || file.isHidden() || !file.exists() || !file.canRead() || !filter.accept(file))
			{
				continue;
			}
			HashMap<String, ArrayList<Document>> resultMap = getInstanceDocsFromFile(file, annotator, docnum);
			for (String entityTypeID : resultMap.keySet())
			{
				ArrayList<Document> resultDocs = resultMap.get(entityTypeID);
				int resDocNum = 0;
				for (Document docToAdd : resultDocs)
				{
					if (docToAdd != null)
					{
						File fileToWrite = new File(destinationPath + "/" + "ldoc" + docnum + "_" + resDocNum + ".txt");
						fileToWrite.createNewFile();
						HashMap<String, String> docAsHash = new HashMap<String, String>();
						for(IndexableField field : docToAdd.getFields())
						{
							docAsHash.put(field.name(), field.stringValue());
							//System.out.println(field.name() + ":" + field.stringValue());
						}
						String json = gson.toJson(docAsHash);
						FileOutputStream fileOut = new FileOutputStream(fileToWrite);
						fileOut.write(json.getBytes());
						fileOut.close();
					}
					resDocNum++;
				}
			}
		}
		annotator.close();
	}
	
	/**
	 * Adds serialized lucene docs to the index
	 * @param start
	 * @param end
	 * @param luceneDocsDirectory
	 */
	@SuppressWarnings("unchecked")
	public void addDocsFromLucenedocDirectory(int start, int end) throws IOException
	{
		for (int docnum = start; docnum < end; docnum++)
		{
			File file = filesToIndex[docnum];
			// if the file is valid
			if (file.isDirectory() || file.isHidden() || !file.exists() || !file.canRead() || !filter.accept(file))
			{
				continue;
			}
			Scanner fileScan = new Scanner(file);
			StringBuilder docText = new StringBuilder();
			while(fileScan.hasNextLine())
				docText.append(fileScan.nextLine());
			Type type = new TypeToken<HashMap<String, String>>(){}.getType();
			HashMap<String, String >docAsHash = gson.fromJson(docText.toString(), type);
			Document luceneDoc = new Document();
			
			FieldType contentType = new FieldType();
			contentType.setStored(false);
			contentType.setTokenized(true);
			contentType.setOmitNorms(false);
			contentType.setIndexOptions(IndexOptions.DOCS_AND_FREQS_AND_POSITIONS_AND_OFFSETS);
			contentType.setStoreTermVectors(false);
			
			String entityTypeID = "ERROR";
			
			for(String fieldName : docAsHash.keySet())
			{
				Field thisField = null;
				switch (fieldName)
				{
					case "content":
						thisField = new Field("content", docAsHash.get(fieldName), contentType);
						break;
					case "_type":
						thisField = new StringField("_type", docAsHash.get(fieldName), Field.Store.YES);
						entityTypeID = docAsHash.get(fieldName);
						break;
					case "name":
						thisField = new StringField("name", docAsHash.get(fieldName), Field.Store.YES);
						break;
					case "size":
						thisField = new LongPoint("size", Integer.parseInt(docAsHash.get(fieldName)));
						break;
					default:
						thisField = new StoredField(fieldName, docAsHash.get(fieldName));
						break;
					
				}
				if(thisField != null)
					luceneDoc.add(thisField);
			}
			if (luceneDoc != null)
			{
				getIndexWriterForType(entityTypeID).addDocument(luceneDoc);
			}
			fileScan.close();
		}
	}
	
	/**
	 * Adds the specified text documents into the IndexWriter after performing annotation with the supplied annotator.
	 * @param start start document index
	 * @param end end document index
	 * @param annotator used to annotate document content
	 * @throws IOException
	 */
	public void addDocsFromTextfileDirectory(int start, int end, NERAnnotator annotator) throws IOException
	{
		// CITATION:
		// https://www.tutorialspoint.com/lucene/lucene_indexing_process.htm
		for (int docnum = start; docnum < end; docnum++)
		{
			try
			{
				File file = filesToIndex[docnum];
				// if the file is valid
				if (file.isDirectory() || file.isHidden() || !file.exists() || !file.canRead() || !filter.accept(file))
				{
					continue;
				}
				HashMap<String, ArrayList<Document>> resultMap = getInstanceDocsFromFile(file, annotator, docnum);
				for (String entityTypeID : resultMap.keySet())
				{
					ArrayList<Document> resultDocs = resultMap.get(entityTypeID);
					for (Document docToAdd : resultDocs)
					{
						if (docToAdd != null)
						{
							getIndexWriterForType(entityTypeID).addDocument(docToAdd);
						}
					}
				}
			}
			catch(Exception e)
			{
				//pass
			}
		}
		annotator.close();
	}
	
	
	/**
	 * Creates a lucene document from a document on disk. NER and tokenization are done here.
	 * @param f the input file
	 * @return
	 */
	private HashMap<String, ArrayList<Document>> getInstanceDocsFromFile(File f, NERAnnotator annotator, int docNum)
	{
		HashMap<String, ArrayList<Document>> docHash = new HashMap<String, ArrayList<Document>>();
		Document doc;
		try
		{
			FieldType contentType = new FieldType();
			contentType.setStored(false);
			contentType.setTokenized(true);
			contentType.setOmitNorms(false);
			contentType.setIndexOptions(IndexOptions.DOCS_AND_FREQS_AND_POSITIONS_AND_OFFSETS);
			contentType.setStoreTermVectors(false);
			//Run the document's content through the NERAnnotator
			annotator.setInput(new String(Files.readAllBytes(f.toPath()), StandardCharsets.UTF_8));
			//String annotation = annotator.getSerializedAnnotation();
			ArrayList<HashMap<String, String>> instanceAnnotations = annotator.getInstanceBasedAnnotations(docNum);
			for(HashMap<String, String> instanceAnnotation: instanceAnnotations)
			{
				doc = new Document();
				
				for(String docKey : instanceAnnotation.keySet())
				{
					String docVal = instanceAnnotation.get(docKey);
					switch(docKey)
					{
						case "_type":
							Field typeField = new StringField(docKey, docVal, Field.Store.YES);
							doc.add(typeField);
							break;
						case "annotationHeader":
							Field nameField = new StringField("name", f.getName() + docVal, Field.Store.YES);
							doc.add(nameField);
							break;
						case "text":
							Field textWindowField = new StoredField(docKey, docVal);
							doc.add(textWindowField);
							break;
						case "entityContent":
							Field entityContentField = new StoredField(docKey, docVal);
							doc.add(entityContentField);
							break;
						default:
							Field tokenField = new Field(docKey, instanceAnnotation.get("annotationHeader") + docVal, contentType);
							doc.add(tokenField);
							break;
					}
				}
				
				BasicFileAttributes attr = Files.readAttributes(f.toPath(), BasicFileAttributes.class);			
				Field sizeField = new LongPoint("size", attr.size());
				Field physicalDocField = new StoredField("physicalDoc",Integer.toString(docNum));
				
				doc.add(sizeField);
				doc.add(physicalDocField);
				
				String entityTypeID = instanceAnnotation.get("_type");
				if(!docHash.containsKey(entityTypeID))
					docHash.put(entityTypeID, new ArrayList<Document>());
				docHash.get(entityTypeID).add(doc);
			}
			
			annotator.getCatalog().resetAnnotations();
			
			return docHash;
			
		}
		catch (IOException e)
		{
			e.printStackTrace();
			return null;
		}
	}
	
	/**
	 * Searches the Inverted index (person) for a simple query
	 * @param queryStr
	 * @throws ParseException
	 * @throws IOException
	 */
	public void runSampleQuery(String queryStr) throws ParseException, IOException
	{
		int numDirectories = indexWriters.size();
		IndexReader[] readers = new IndexReader[numDirectories];
		int readerNum = 0;
		for(IndexWriter writer : indexWriters.values())
		{
			readers[readerNum] = DirectoryReader.open(writer.getDirectory());
			readerNum++;
		}
	    IndexSearcher searcher = new IndexSearcher(new MultiReader(readers));
	    TopScoreDocCollector collector = TopScoreDocCollector.create(10);
	    //SpanQuery q1  = new SpanTermQuery(new Term("versions of macos", "mountain lion"));
	    
	    
	    
	    
	    
	    
	    
	    WildcardQuery wildcard = new WildcardQuery(new Term("ios device", "*"));
	    SpanQuery spanWildcard = new SpanMultiTermQueryWrapper<WildcardQuery>(wildcard);
	    SpanQuery q3  = new SpanTermQuery(new Term("keyword", "car"));
	    SpanQuery q4  = new SpanTermQuery(new Term("keyword", "stereo"));
	    SpanQuery q1m = new FieldMaskingSpanQuery(spanWildcard, "keyword");
	    Query q = new SpanNearQuery(new SpanQuery[]{q1m, q3, q4}, 10, false);
	    
	    
	    
	    
	    
	    
	    //MultiFieldPhraseQuery.Builder builder = new MultiFieldPhraseQuery.Builder();
	    //builder.add(new Term("versions of macos", "mountain lion"), 0);
	    //builder.add(new Term("keyword", "optional"), 1);
	    //builder.add(new Term("keyword", "isnt"), 0);
	    //builder.add(new Term("keyword", "running"), 1);
	    //MultiFieldPhraseQuery q = builder.build();
	    //Query q = new QueryParser("keyword", new StandardAnalyzer()).parse(queryStr);
	    searcher.search(q, collector);
	    ScoreDoc[] hits = collector.topDocs().scoreDocs;
	    
	    //	Code to display the results of search
	    System.out.println("Found " + hits.length + " hits.");
	    for(int i=0;i<hits.length;++i) 
	    {
	      int docId = hits[i].doc;
	      Document d = searcher.doc(docId);
	      System.out.println((i + 1) + ". " + d.get("name"));
	      //String annString = d.get("annotation");
	      //System.out.println(annString.substring(0, annString.length()/3));
	      System.out.println(d.get("text"));
	      System.out.println();
	    }
	    
	    // reader can only be closed when there is no need to access the documents any more
	    searcher.getIndexReader().close();
	}
	
	/**
	 * This flushes everything to disk.
	 * @throws IOException
	 */
	public void closeWriters() throws IOException
	{
		for(String entityTypeID : indexWriters.keySet())
		{
			indexWriters.get(entityTypeID).close();
		}
	}

	/**
	 * TODO
	 * @param catalogClone
	 * @throws IOException 
	 */
	public void createIndexWriters(EntityCatalog catalogClone) throws IOException
	{
		ArrayList<String> entityTypes = catalogClone.getEntityTypes();
		if(!splitIndex)
		{
			entityTypes = new ArrayList<String>();
			entityTypes.add("all");
		}
		for(String entityTypeID : entityTypes)
		{
			Directory indexDirectory = new SimpleFSDirectory(Paths.get("src/indexdir/" + entityTypeID));
			NERAnalyzer nerAnalyzer = new NERAnalyzer();
			nerAnalyzer.setTokenizer(new InstanceInvNERTokenizer(inputFormat, outputFormat, instancePosition, entityPayloadFields, termPayloadFields));
			IndexWriterConfig nerConfig = new IndexWriterConfig(nerAnalyzer);
			//Use for readable postings
			if(useSimpleText)
				nerConfig.setCodec(new SimpleTextCodec());
			nerConfig.setOpenMode(IndexWriterConfig.OpenMode.CREATE);
			IndexWriter typeIndexWriter = new IndexWriter(indexDirectory, nerConfig);
			indexWriters.put(entityTypeID, typeIndexWriter);
		}
	}
	
}
