package indexing;

import java.io.IOException;
import java.util.concurrent.Callable;

import ner.annotation.NERAnnotator;

/**
 * Represents one thread of annotation / indexing.
 * @author alexaulabaugh
 */

public class Ingester implements Callable<Double>
{

	private NERAnnotator annotator;
	private IndexingManager indexer;
	private int docMin;
	private int docMax;
	
	public Ingester(){}
	
	/**
	 * Sets this thread's NERAnnotator
	 * @param a
	 */
	public void setAnnotator(NERAnnotator a)
	{
		annotator = a;
	}
	
	/**
	 * The interface for writing to index
	 * @param i
	 */
	public void setIndexingManager(IndexingManager i)
	{
		indexer = i;
	}
	
	/**
	 * This will be the doc numbers indexed by this thread
	 * @param min
	 * @param max
	 */
	public void setDocumentRange(int min, int max)
	{
		docMin = min;
		docMax = max;
	}

	@Override
	public Double call() throws Exception
	{
		long startTime = System.currentTimeMillis();
		try
		{
			//indexer.annotateDocsFromTextfileDirectory(docMin, docMax, annotator, "luceneDocs");
			//indexer.addDocsFromLucenedocDirectory(docMin, docMax);
			//indexer.addDocsFromTextfileDirectory(docMin, docMax, annotator);
			indexer.addDocsFromDirectory(docMin, docMax, annotator);
		}
		catch (IOException e)
		{
			e.printStackTrace();
		}
		long endTime = System.currentTimeMillis();
		return (endTime - startTime)*0.001;
	}

}
