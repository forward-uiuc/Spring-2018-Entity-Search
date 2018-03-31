package ingestion;

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
	private IngestionManager indexer;
	private int docMin;
	private int docMax;
	private int ID;
	
	public Ingester(int id)
	{
		ID = id;
	}
	
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
	public void setIndexingManager(IngestionManager i)
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
			indexer.addDocsFromDirectory(docMin, docMax, annotator, ID);
		}
		catch (IOException e)
		{
			e.printStackTrace();
		}
		long endTime = System.currentTimeMillis();
		return (endTime - startTime)*0.001;
	}

}
