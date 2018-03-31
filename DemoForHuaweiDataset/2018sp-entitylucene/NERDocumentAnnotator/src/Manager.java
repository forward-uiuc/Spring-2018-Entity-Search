import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.math.RoundingMode;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import ingestion.IngestionManager;
import ingestion.Ingester;
import ingestion.WholeDocumentIngestionManager;
import ner.annotation.AnnotationManager;

/*
 * CITATION: https://github.com/sreejithc321/Lucene-Java/blob/master/LuceneDemo.java
 */

/**
 * Creates and runs document ingestion threads. These perform NE Annotation and write to the index
 * @author aaulabaugh@gmail.com
 */
public class Manager 
{
	//The encoding type for entity annotations (if not using ES annotations)
	private static final String ANNOTATION_OUTPUT_FORMAT = "JSON"; //JSON or encodedString
	
	//Whether the entity types are to be split into different indexes. This controls whether the _type field is set.
	private static final boolean SPLIT_INDEX = false;
	
	//Number of threads used to index
	private static final int NUM_THREADS = 1;
	//Number of docs to index (set <= to number of docs in folder)
	private static final int NUM_DOCS = 30000;
	
	//The defined term num for the entity instance.
	private static final int INSTANCE_TERM_POSITION = 10;
	
	//The number of annotations recorded on either side of an entity occurrence
	private static final int ANNOTATION_WINDOW_SIZE = 10;
	
	//D or E         
	private static final String DOC_TYPE = "E";
	
	//true for simple ES annotations
	private static final boolean USE_ES = true;
	
	//Whether to use the Huawei input format
	private static final boolean USE_HUAWEI = true;
	private static String HUAWEI_INPUT;
	private static String HUAWEI_DIRECTORY;
	
	//All the threads
	private static Collection<Ingester> ingestionThreads;
		
	//Builds an index, and queries it once
	public static void main(String[] args) throws Exception
	{
		final String[] ENTITY_PAYLOAD_FIELDS;
		final String[] TERM_PAYLOAD_FIELDS;
		IngestionManager indexer = null;
		int docsPerThread = NUM_DOCS/NUM_THREADS;

		
		String destinationPath = "";
		if(USE_ES)
			destinationPath = "src/indexdir/ESDocs";
		else
			destinationPath = "src/indexdir/luceneDocs";
		
		//Load LOCALPATHS
		String path = new File(".").getAbsolutePath();
		String[] pathArr = path.split("/");
		path = String.join("/", Arrays.copyOfRange(pathArr, 0, pathArr.length-2));
		String localPathsFile = path + "/LOCALPATHS_ANNOTATOR.txt";
		List<String> lines = Files.readAllLines(Paths.get(localPathsFile));
		for(int i = 0; i < lines.size(); i++)
		{
			String[] line = lines.get(i).split(":");
			if(line[0].equals("HUAWEIDOCS"))
			{
				HUAWEI_INPUT = line[1];
				HUAWEI_DIRECTORY = "";
				String[] huaweiPath = line[1].split("/");
				for(int j = 0; j< huaweiPath.length-1; j++)
					HUAWEI_DIRECTORY += huaweiPath[j] + "/";
				HUAWEI_DIRECTORY += "intermediate";
			}
		}
		
		if(USE_HUAWEI)
		{
			Files.createDirectories(Paths.get(HUAWEI_DIRECTORY));
			while(!(new File(HUAWEI_DIRECTORY).exists()))
				Thread.sleep(1000);
			//All documents in one file
			BufferedReader huaweiFile = new BufferedReader(new FileReader(HUAWEI_INPUT));
			String huaweiDoc = "";
			int docNum = 0;
			int threadNum = 0;
			FileOutputStream huaweiIntermediateOut = new FileOutputStream(HUAWEI_DIRECTORY + "/intermediate_" + threadNum + ".txt");
			while((huaweiDoc = huaweiFile.readLine()) != null && docNum < NUM_DOCS)
			{
				if(docNum % docsPerThread == 0 && docNum > 0)
				{
					huaweiIntermediateOut.close();
					threadNum++;
					huaweiIntermediateOut = new FileOutputStream(HUAWEI_DIRECTORY + "/intermediate_" + threadNum + ".txt");
				}
				huaweiIntermediateOut.write((huaweiDoc + "\n").getBytes());
				docNum++;
			}
			huaweiIntermediateOut.close();
			huaweiFile.close();
		}
		
		if(DOC_TYPE.equals("D"))
		{
			ENTITY_PAYLOAD_FIELDS = new String[]{"instance"};;
			TERM_PAYLOAD_FIELDS = new String[]{};
			indexer = new WholeDocumentIngestionManager(destinationPath, ENTITY_PAYLOAD_FIELDS, TERM_PAYLOAD_FIELDS, USE_ES, USE_HUAWEI);
		}
		else if(DOC_TYPE.equals("E"))
		{
			ENTITY_PAYLOAD_FIELDS = new String[]{"docEntityType", "instance", "physicalDocNum"};
			TERM_PAYLOAD_FIELDS = new String[]{"docEntityType", "physicalDocNum"};
			indexer = new IngestionManager(INSTANCE_TERM_POSITION, SPLIT_INDEX, destinationPath, ENTITY_PAYLOAD_FIELDS, TERM_PAYLOAD_FIELDS, USE_ES, USE_HUAWEI);
		}
		else
		{
			throw new Exception("Invalid DOC_TYPE");
		}
		indexer.createOutputDir(destinationPath);
		
		DecimalFormat df = new DecimalFormat("###.###");
		df.setRoundingMode(RoundingMode.HALF_DOWN);
		ingestionThreads = new ArrayList<Ingester>();
		AnnotationManager annotationManager = new AnnotationManager(ANNOTATION_WINDOW_SIZE, USE_ES);
		annotationManager.setUpCatalog();
		ExecutorService exec = Executors.newFixedThreadPool(NUM_THREADS);
		
		//Set up ingesters
		for(int i = 0; i < NUM_THREADS; i++) 
		{
			Ingester newIngester = new Ingester(i);
			newIngester.setAnnotator(annotationManager.getAnnotator(ANNOTATION_OUTPUT_FORMAT));
			if(USE_HUAWEI)
			{
				System.out.println("THREAD " + i + " ASSIGNED TO DOCS [" + docsPerThread*i + ", " + docsPerThread*(i+1) + ")");
				newIngester.setDocumentRange(0, docsPerThread);
			}
			else
			{
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
			}
			newIngester.setIndexingManager(indexer);
			ingestionThreads.add(newIngester);
		}
		System.out.println();
		System.out.println("BEGIN ANNOTATION");
		long startTime = System.currentTimeMillis();
		List<Future<Double>> threadOut = exec.invokeAll(ingestionThreads);
		for(Future<Double> f : threadOut)
		{
			System.out.println("THREAD FINISHED IN " + df.format(f.get()) + " SEC");
		}
		long endTime = System.currentTimeMillis();
		System.out.println();
		System.out.println("INDEXED " + NUM_DOCS + " DOCS WITH " + NUM_THREADS + " THREAD(S) IN: " + df.format((endTime - startTime)*0.001) + " Seconds");
		exec.shutdown();
	}
}
