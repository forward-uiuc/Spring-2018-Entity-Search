import java.io.IOException;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.apache.lucene.queryparser.classic.ParseException;

import indexing.IndexingManager;
import indexing.Ingester;
import indexing.WholeDocumentIndexingManager;
import ner.annotation.AnnotationManager;

/*
 * CITATION: https://github.com/sreejithc321/Lucene-Java/blob/master/LuceneDemo.java
 */

/**
 * Creates and runs document ingestion threads. These perform NE Annotation and write to the index
 * @author aaulabaugh@gmail.com
 */
public class IngestionManager 
{
	//The encoding type for entity annotations
	//Use JSON and turn on USE_SIMPLE_TEXT_CODEC for a readable format
	//Use encodedString to allow entity queries to work (there is a bug with JSON)
	private static final String ANNOTATION_OUTPUT_FORMAT = "JSON"; //JSON or encodedString
	private static final String INDEXING_OUTPUT_FORMAT = "JSON"; //JSON or encodedString
	private static final Boolean USE_SIMPLE_TEXT_CODEC = true;
	private static final Boolean SPLIT_INDEX = false;
	
	//Number of threads used to index
	private static final int NUM_THREADS = 1;
	//Number of docs to index (set <= to number of docs in folder)
	private static final int NUM_DOCS = 1;
	
	//The defined term num for the entity instance.
	private static final int INSTANCE_TERM_POSITION = 10;
	
	//The number of annotations recorded on either side of an entity occurrence
	private static final int ANNOTATION_WINDOW_SIZE = 10;
	
	//D or E
	private static final String DOC_TYPE = "E";
	
	//All the threads
	private static Collection<Ingester> ingestionThreads;
		
	//Builds an index, and queries it once
	public static void main(String[] args) throws Exception
	{
		final String[] ENTITY_PAYLOAD_FIELDS;
		final String[] TERM_PAYLOAD_FIELDS;
		IndexingManager indexer = null;
		if(DOC_TYPE.equals("D"))
		{
			ENTITY_PAYLOAD_FIELDS = new String[]{"instance"};;
			TERM_PAYLOAD_FIELDS = new String[]{};
			indexer = new WholeDocumentIndexingManager(ANNOTATION_OUTPUT_FORMAT, INDEXING_OUTPUT_FORMAT, USE_SIMPLE_TEXT_CODEC, ENTITY_PAYLOAD_FIELDS, TERM_PAYLOAD_FIELDS);
		}
		else if(DOC_TYPE.equals("E"))
		{
			ENTITY_PAYLOAD_FIELDS = new String[]{"docEntityType", "instance", "physicalDocNum"};
			TERM_PAYLOAD_FIELDS = new String[]{"docEntityType", "physicalDocNum"};
			indexer = new IndexingManager(ANNOTATION_OUTPUT_FORMAT, INDEXING_OUTPUT_FORMAT, USE_SIMPLE_TEXT_CODEC, INSTANCE_TERM_POSITION, SPLIT_INDEX, ENTITY_PAYLOAD_FIELDS, TERM_PAYLOAD_FIELDS);
		}
		else
		{
			throw new Exception("Invalid DOC_TYPE");
		}
		
		DecimalFormat df = new DecimalFormat("###.###");
		df.setRoundingMode(RoundingMode.HALF_DOWN);
		ingestionThreads = new ArrayList<Ingester>();
		AnnotationManager annotationManager = new AnnotationManager(ANNOTATION_WINDOW_SIZE);
		annotationManager.setUpCatalog();
		indexer.createIndexWriters(annotationManager.getCatalogClone());
		int docsPerThread = NUM_DOCS/NUM_THREADS;
		ExecutorService exec = Executors.newFixedThreadPool(NUM_THREADS);
		for(int i = 0; i < NUM_THREADS; i++)
		{
			Ingester newIngester = new Ingester();
			newIngester.setAnnotator(annotationManager.getAnnotator(ANNOTATION_OUTPUT_FORMAT));
			if(i == (NUM_THREADS-1) && docsPerThread*(i-1) < NUM_DOCS)
			{
				System.out.println("THREAD " + i + " ASSIGNED TO DOCS [" + docsPerThread*i + ", " + NUM_DOCS + ")");
				newIngester.setDocumentRange(docsPerThread*i, NUM_DOCS);
			}
			else
			{
				System.out.println("THREAD " + i + " ASSIGNED TO DOCS [" + docsPerThread*i + ", " + docsPerThread*(i+1) + ")");
				newIngester.setDocumentRange(docsPerThread*i, docsPerThread*(i+1));
			}
			newIngester.setIndexingManager(indexer);
			ingestionThreads.add(newIngester);
		}
		System.out.println();
		System.out.println("BEGIN INDEXING");
		long startTime = System.currentTimeMillis();
		List<Future<Double>> threadOut = exec.invokeAll(ingestionThreads);
		for(Future<Double> f : threadOut)
		{
			System.out.println("THREAD FINISHED IN " + df.format(f.get()) + " SEC");
		}
		indexer.closeWriters();
		long endTime = System.currentTimeMillis();
		System.out.println();
		System.out.println("INDEXED " + NUM_DOCS + " DOCS WITH " + NUM_THREADS + " THREAD(S) IN: " + df.format((endTime - startTime)*0.001) + " Seconds");
		exec.shutdown();
		try
		{
			System.out.println();
			String query = "screen";
			System.out.println("Running query");
			indexer.runSampleQuery(query);
		}
		catch (ParseException e)
		{
			e.printStackTrace();
		}
	}
}
