import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.attribute.BasicFileAttributes;
import java.text.DecimalFormat;
import java.util.Arrays;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import org.apache.lucene.codecs.simpletext.SimpleTextCodec;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.FieldType;
import org.apache.lucene.document.StringField;
import org.apache.lucene.document.LongPoint;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexOptions;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.SimpleFSDirectory;

import formatting.Entity;
import formatting.TokenField;
import indexing.TextFileFilter;
import ner.analysis.InstanceInvNERTokenizer;
import ner.analysis.NERAnalyzer;
import ner.annotation.AnnotationManager;
import ner.annotation.EntityCatalog;
import ner.annotation.EntityInstance;
import ner.annotation.EntityType;
import ner.annotation.GazetteerTable;
import ner.annotation.NERAnnotator;
import ner.annotation.RootsOnlyReconciler;
import ner.annotation.extraction.GazetteerTableExtractor;
import ner.annotation.extraction.PhoneEmailExtractor;
import ner.annotation.extraction.StanfordExtractor;
import query.Aggregator;
import query.EntityIndexSearcher;
import query.EntityScoreDoc;
import query.EntityTermQuery;
import query.PatternQueryHandler;

/*
 * CITATION: https://github.com/sreejithc321/Lucene-Java/blob/master/LuceneDemo.java
 */

/**
 * Controls the indexing process
 * @author aaulabaugh@gmail.com
 */

public class IndexingManagerQuery 
{
	
	//The tool used to tokenize a field and annotate it with entities
	private static NERAnnotator annotator;
	
	//The documents to index
	private static String localDocs;
	
	//Builds an index, and queries it once
	public static void main(String[] args)
	{
		try
		{
			//Get local path
			String path = new File(".").getAbsolutePath();
			String[] pathArr = path.split("/");
			path = String.join("/", Arrays.copyOfRange(pathArr, 0, pathArr.length-2));
			String localPathsFile = path + "/LOCALPATHS.txt";
			List<String> lines = Files.readAllLines(Paths.get(localPathsFile));
			localDocs = lines.get(2).split(":")[1];
			String classifierPath = lines.get(3).split(":")[1];
			
			AnnotationManager manager = new AnnotationManager(100);
			manager.setUpCatalog();
			annotator = manager.getAnnotator("encodedString");
			
			//D-Inverted Index
			Directory indexDirectoryD = new SimpleFSDirectory(Paths.get("src/indexdirD"));
			NERAnalyzer nerAnalyzerD = new NERAnalyzer();
			nerAnalyzerD.setTokenizer(new InstanceInvNERTokenizer("encodedString", "encodedString", 1000, new String[]{}, new String[]{})); //d-inverted index
			IndexWriterConfig nerConfigD = new IndexWriterConfig(nerAnalyzerD);
			nerConfigD.setCodec(new SimpleTextCodec());
			nerConfigD.setOpenMode(IndexWriterConfig.OpenMode.CREATE);
			IndexWriter dinvWriter = new IndexWriter(indexDirectoryD, nerConfigD);
			
			//CITATION: https://stackoverflow.com/questions/153724/how-to-round-a-number-to-n-decimal-places-in-java
			DecimalFormat df = new DecimalFormat("###.###");
			df.setRoundingMode(RoundingMode.HALF_DOWN);
			
			//Index documents
			long startTime = System.currentTimeMillis();
			addDocsFromDirectory(localDocs, new TextFileFilter(), dinvWriter);
			dinvWriter.close();
			long endTime = System.currentTimeMillis();
			System.out.println();
			System.out.println("INDEXED DOCS IN: " + df.format((endTime - startTime)*0.001) + " Seconds");
			System.out.println();
			//modified
		}
		catch(Exception e)
		{
			e.printStackTrace();
		}
	}
	
	/**
	 * Add some EntityTypes
	 * @param cat
	 */
	private static void addEntityTypes(EntityCatalog cat)
	{
		//Define all entities
		EntityType person = new EntityType("PERSON");
		EntityType location = new EntityType("LOCATION");
		EntityType organization = new EntityType("ORGANIZATION");
		EntityType phone = new EntityType("PHONE");
		EntityType email = new EntityType("EMAIL");
		EntityType state = new EntityType("STATE");
		EntityType country = new EntityType("COUNTRY");
		
		//Add super-entities
		state.addSuperType(location);
		country.addSuperType(location);
		
		cat.addEntityType(person);
		cat.addEntityType(location);
		cat.addEntityType(organization);
		cat.addEntityType(phone);
		cat.addEntityType(email);
		cat.addEntityType(state);
		cat.addEntityType(country);
	}
	
	/**
	 * Adds gazetteer entities to the NERAnnotator.
	 * These could be read in from a file.
	 * @param annotator
	 */
	private static void addGazetteerEntities(EntityCatalog catalog)
	{		
		EntityInstance sampleInstance = new EntityInstance("State:Illinois");
		sampleInstance.addType(catalog.getEntityType("STATE"));
		sampleInstance.addSynonym("Illinois");
		sampleInstance.addSynonym("IL");
		catalog.getGazetteerTable().addInstance(sampleInstance);
		
		EntityInstance sampleInstance2 = new EntityInstance("Country:USA");
		sampleInstance2.addType(catalog.getEntityType("COUNTRY"));
		sampleInstance2.addSynonym("USA");
		sampleInstance2.addSynonym("U.S.A.");
		sampleInstance2.addSynonym("U.S.");
		sampleInstance2.addSynonym("United States");
		sampleInstance2.addSynonym("United States of America");
		sampleInstance2.addSynonym("America");
		catalog.getGazetteerTable().addInstance(sampleInstance2);
	}
	
	/**
	 * Adds all text documents recursively in a directory to the indexWriter.
	 * @param path
	 * @param filter the specifications of which files are acceptable to index
	 * @param indexD the d-inverted indexWriter
	 * @param indexE the e-inverted indexWriter
	 * @throws IOException
	 */
	private static void addDocsFromDirectory(String path, FileFilter filter, IndexWriter indexD) throws IOException
	{
		//CITATION: https://www.tutorialspoint.com/lucene/lucene_indexing_process.htm
		File[] files = new File(path).listFiles();
		for (File file : files)
		{
			//if the file is valid
	         if(!file.isDirectory() && !file.isHidden() && file.exists() && file.canRead() && filter.accept(file))
	         {
	            Document[] resultDocs = getDocFromFile(file);
	            Document docToAdd = resultDocs[0];
	            if(docToAdd != null)
	            {
	            	indexD.addDocument(docToAdd);
	            }
	         }
	         else if(file.isDirectory())
	         {
	        	 addDocsFromDirectory(file.getPath(), filter, indexD);
	         }
	      }
	}
	
	/**
	 * Creates a lucene document from a document on disk. NER and tokenization are done here.
	 * @param f the input file
	 * @return
	 */
	private static Document[] getDocFromFile(File f)
	{
		Document[] returnDocuments = new Document[2];
		Document doc = new Document();
		try
		{
			FieldType contentType = new FieldType();
			contentType.setStored(false);
			contentType.setTokenized(true);
			contentType.setOmitNorms(false);
			contentType.setIndexOptions(IndexOptions.DOCS_AND_FREQS_AND_POSITIONS_AND_OFFSETS);
			contentType.setStoreTermVectors(true);
			//Run the document's content through the NERAnnotator
			annotator.setInput(new String(Files.readAllBytes(f.toPath()), StandardCharsets.UTF_8));
			//String annotation = annotator.getSerializedAnnotation();
			String annotation = annotator.getSerializedAnnotation();
			Field contentField = new Field("content", annotation, contentType);
			
			Field nameField = new StringField("name", f.getName(), Field.Store.YES);
			
			BasicFileAttributes attr = Files.readAttributes(f.toPath(), BasicFileAttributes.class);			
			Field sizeField = new LongPoint("size", attr.size());
			
			doc.add(contentField);
			doc.add(nameField);
			doc.add(sizeField);
			returnDocuments[0] = doc;
			
			return returnDocuments;
			
		}
		catch (IOException e)
		{
			e.printStackTrace();
			return null;
		}
	}
}