package ingestion;

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
import java.util.concurrent.locks.ReentrantLock;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import java.lang.reflect.Type;

import ner.annotation.EntityCatalog;
import ner.annotation.NERAnnotator;

/**
 * Interface for writing to the indexes.
 * TODO make configurable (command line?)
 * @author alexaulabaugh
 */

public class IngestionManager
{	
	//The files to index
	protected static File[] filesToIndex;
	
	//Filters filetypes
	protected static FileFilter filter;
							
	protected static int instancePosition;
	
	protected static boolean splitIndex;
	
	protected static boolean USE_ES;
	
	protected static boolean USE_HUAWEI;
					
	protected String[] entityPayloadFields;
	protected String[] termPayloadFields;
	
	protected Gson gson;
	
	protected String destinationPath;
			
	public IngestionManager(int instPos, Boolean split, String destPath, String[] entityPayloadFields, String[] termPayloadFields, boolean use_es, boolean use_huawei)
	{
		try
		{
			USE_HUAWEI = use_huawei;
			USE_ES = use_es;
			instancePosition = instPos;
			splitIndex = split;
			this.entityPayloadFields = entityPayloadFields;
			this.termPayloadFields = termPayloadFields;
			
			filter = new TextFileFilter();
			//Get local path
			String path = new File(".").getAbsolutePath();
			String[] pathArr = path.split("/");
			path = String.join("/", Arrays.copyOfRange(pathArr, 0, pathArr.length-2));
			String localPathsFile = path + "/LOCALPATHS_ANNOTATOR.txt";
			List<String> lines = Files.readAllLines(Paths.get(localPathsFile));
			String localDocs = "";
			String huaweiDocs = "";
			for(int i = 0; i < lines.size(); i++)
			{
				String[] line = lines.get(i).split(":");
				if(line[0].equals("LOCALDOCS"))
					localDocs = line[1];
				else if(line[0].equals("HUAWEIDOCS"))
				{
					String[] huaweiPath = line[1].split("/");
					for(int j = 0; j< huaweiPath.length-1; j++)
						huaweiDocs += huaweiPath[j] + "/";
					huaweiDocs += "intermediate";
				}
			}
			if(USE_HUAWEI)
				filesToIndex = new File(huaweiDocs).listFiles(new FileFilter() 
				{
					//CITATION: https://stackoverflow.com/questions/15646358/how-to-list-only-non-hidden-and-non-system-file-in-jtree
					@Override
					public boolean accept(File pathname)
					{
						return !pathname.isHidden();
					}
					
				});
			else
				filesToIndex = new File(localDocs).listFiles(new FileFilter() 
				{
					//CITATION: https://stackoverflow.com/questions/15646358/how-to-list-only-non-hidden-and-non-system-file-in-jtree
					@Override
					public boolean accept(File pathname)
					{
						return !pathname.isHidden();
					}
					
				});
			
			
			destinationPath = destPath;
			
			gson = new GsonBuilder().create();
		}
		catch(IOException e)
		{
			e.printStackTrace();
		}
	}
	
	
	/**
	 * Entry point for adding documents from a directory.
	 * @param start
	 * @param end
	 * @param annotator
	 * @throws Exception
	 */
	public void addDocsFromDirectory(int start, int end, NERAnnotator annotator, int threadNum) throws Exception
	{
		annotateDocsFromTextfileDirectory(start, end, annotator, threadNum);
	}
	
	/**
	 * Creates the output dir if it does not exist
	 * @param destinationPath
	 */
	public void createOutputDir(String destinationPath)
	{
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
	}
	
	/**
	 * perform NER on plaintext docs
	 * @param start
	 * @param end
	 * @param annotator
	 * @param destination
	 * @throws IOException
	 */
	public void annotateDocsFromTextfileDirectory(int start, int end, NERAnnotator annotator, int threadNum) throws IOException
	{
		// CITATION:
		// https://www.tutorialspoint.com/lucene/lucene_indexing_process.htm
		if(USE_ES)
		{
			File fileToWrite = new File(destinationPath + "/" + "esdata" + threadNum + ".json");
			//System.out.println(fileToWrite.getAbsolutePath());
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
				//System.out.println("docnum: "+docnum);
				HashMap<String, ArrayList<HashMap<String, String>>> resultMap = getInstanceDocsFromFile(file, annotator, docnum);
				for (String entityTypeID : resultMap.keySet())
				{
					ArrayList<HashMap<String, String>> resultDocs = resultMap.get(entityTypeID);
					for (HashMap<String, String> docToAdd : resultDocs)
					{
						if (docToAdd != null)
						{
							fileOut.write("{\"index\": {\"_type\": \"e_document\"}}\n".getBytes());
							String json = gson.toJson(docToAdd) + "\n";
							fileOut.write(json.getBytes());
						}
					}
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
				HashMap<String, ArrayList<HashMap<String, String>>> resultMap = getInstanceDocsFromFile(file, annotator, docnum);
				for (String entityTypeID : resultMap.keySet())
				{
					ArrayList<HashMap<String, String>> resultDocs = resultMap.get(entityTypeID);
					int resDocNum = 0;
					for (HashMap<String, String> docToAdd : resultDocs)
					{
						if (docToAdd != null)
						{
							File fileToWrite = new File(destinationPath + "/" + "ldoc" + docnum + "_" + resDocNum + ".txt");
							fileToWrite.createNewFile();
							String json = gson.toJson(docToAdd);
							FileOutputStream fileOut = new FileOutputStream(fileToWrite);
							fileOut.write(json.getBytes());
							fileOut.close();
						}
						resDocNum++;
					}
				}
			}
		}
		annotator.close();
	}
	
	
	/**
	 * Creates a lucene document from a document on disk. NER and tokenization are done here.
	 * @param f the input file
	 * @return
	 */
	private HashMap<String, ArrayList<HashMap<String, String>>> getInstanceDocsFromFile(File f, NERAnnotator annotator, int docNum)
	{
		HashMap<String, ArrayList<HashMap<String, String>>> docHash = new HashMap<String, ArrayList<HashMap<String, String>>>();
		try
		{
			//Run the document's content through the NERAnnotator
			//System.out.println(f.getAbsolutePath());
			if(USE_HUAWEI)
			{
				annotator.setInput(Integer.toString(docNum) + "," + f.getAbsolutePath());
			}
			else
				annotator.setInput(new String(Files.readAllBytes(f.toPath()), StandardCharsets.UTF_8));
			//String annotation = annotator.getSerializedAnnotation();
			//System.out.println("docNum: "+docNum);
			ArrayList<HashMap<String, String>> instanceAnnotations = annotator.getInstanceBasedAnnotations(docNum);
			for(HashMap<String, String> instanceAnnotation: instanceAnnotations)
			{
				HashMap<String, String> doc = new HashMap<String, String>();
				for(String docKey : instanceAnnotation.keySet())
				{
					doc.put(docKey, instanceAnnotation.get(docKey));
				}
				String entityTypeID = instanceAnnotation.get("_type");
				doc.put("name", f.getName());
				//System.out.print("name: "+f.getName());
				BasicFileAttributes attr = Files.readAttributes(f.toPath(), BasicFileAttributes.class);	
				doc.put("size", Long.toString(attr.size()));
				doc.put("physicalDoc", Integer.toString(docNum));
				
				if(!docHash.containsKey(entityTypeID))
					docHash.put(entityTypeID, new ArrayList<HashMap<String, String>>());
				docHash.get(entityTypeID).add(doc);
			}
			
			annotator.getCatalog().resetAnnotations();
			//System.out.println("docHash:" + docHash);
			return docHash;
			
		}
		catch (IOException e)
		{
			e.printStackTrace();
			return null;
		}
	}
	
}
