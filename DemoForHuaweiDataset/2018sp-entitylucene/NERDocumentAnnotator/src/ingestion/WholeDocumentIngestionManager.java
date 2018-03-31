package ingestion;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Scanner;
import java.util.concurrent.locks.ReentrantLock;

import com.google.gson.reflect.TypeToken;

import ner.annotation.EntityCatalog;
import ner.annotation.NERAnnotator;

public class WholeDocumentIngestionManager extends IngestionManager
{
	public WholeDocumentIngestionManager(String destPath, String[] entityPayloadFields, String[] termPayloadFields, boolean use_es, boolean use_huawei)
	{
		super(-1, false, destPath, entityPayloadFields, termPayloadFields, use_es, use_huawei);
	}
	
	
	@Override
	public void annotateDocsFromTextfileDirectory(int start, int end, NERAnnotator annotator, int threadNum) throws IOException
	{
		// CITATION:
		// https://www.tutorialspoint.com/lucene/lucene_indexing_process.htm
		if(USE_ES)
		{
			File fileToWrite = new File(destinationPath + "/" + "esdata" + threadNum + ".json");
			fileToWrite.createNewFile();
			FileOutputStream fileOut = new FileOutputStream(fileToWrite);
			for (int docnum = start; docnum < end; docnum++)
			{
				File file;
				if(USE_HUAWEI)
					file = filesToIndex[threadNum];
				else
					file = filesToIndex[docnum];
				// if the file is valid
				if (file.isDirectory() || file.isHidden() || !file.exists() || !file.canRead() || !filter.accept(file))
				{
					continue;
				}
				HashMap<String, String> luceneDoc = getDocFromFile(file, annotator, docnum);
				if (luceneDoc != null)
				{
					fileOut.write("{\"index\": {\"_type\": \"d_document\"}}\n".getBytes());
					String json = gson.toJson(luceneDoc) + "\n";
					fileOut.write(json.getBytes());
				}
			}
			fileOut.close();
		}
		else
		{
			for (int docnum = start; docnum < end; docnum++)
			{
				File file;
				if(USE_HUAWEI)
					file = filesToIndex[threadNum];
				else
					file = filesToIndex[docnum];
				// if the file is valid
				if (file.isDirectory() || file.isHidden() || !file.exists() || !file.canRead() || !filter.accept(file))
				{
					continue;
				}
				HashMap<String, String> luceneDoc = getDocFromFile(file, annotator, docnum);
				if (luceneDoc != null)
				{
					File fileToWrite = new File(destinationPath + "/" + "ldoc" + docnum + ".txt");
					fileToWrite.createNewFile();
					String json = gson.toJson(luceneDoc);
					FileOutputStream fileOut = new FileOutputStream(fileToWrite);
					fileOut.write(json.getBytes());
					fileOut.close();
				}
			}
		}
		annotator.close();
	}
	
	/**
	 * Gets a Lucene Document from a textfile on disk
	 * @param f the textfile
	 * @param annotator
	 * @param docNum
	 * @return
	 */
	private HashMap<String, String> getDocFromFile(File f, NERAnnotator annotator, int docNum)
	{
		HashMap<String, String> doc = new HashMap<String, String>();
		try
		{
			
			if(USE_HUAWEI)
			{
				annotator.setInput(Integer.toString(docNum) + "," + f.getAbsolutePath());
			}
			else
				annotator.setInput(new String(Files.readAllBytes(f.toPath()), StandardCharsets.UTF_8));
			
			HashMap<String, String> fieldAnnotations = annotator.getFieldBasedAnnotation();
			
			for(String entityType : fieldAnnotations.keySet())
			{
				if(entityType == "text")
				{
					doc.put("text", fieldAnnotations.get(entityType));
				}
				else
				{
					doc.put(entityType, fieldAnnotations.get(entityType));
				}
			}
			//System.out.println("name: "+f.getName());
			doc.put("name", f.getName());
			BasicFileAttributes attr = Files.readAttributes(f.toPath(), BasicFileAttributes.class);
			doc.put("size", Long.toString(attr.size()));
			
		}
		catch(Exception e)
		{
			e.printStackTrace();
			return null;
		}
		return doc;
	}
	
}
