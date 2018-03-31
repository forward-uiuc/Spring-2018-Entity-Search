package ner.annotation;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;

import edu.stanford.nlp.ling.tokensregex.TokenSequencePattern;
import ner.annotation.extraction.DBPediaExtractor;
import ner.annotation.extraction.GazetteerTableExtractor;
import ner.annotation.extraction.GazetteerTreeExtractor;
import ner.annotation.extraction.GolfExtractor;
import ner.annotation.extraction.PhoneEmailExtractor;
import ner.annotation.extraction.StanfordExtractor;
import ner.annotation.extraction.StanfordGazetteerExtractor;
import ner.annotation.extraction.StanfordGazetteerPatternBuilder;
import ner.annotation.treegazetteer.GazetteerTree;

/**
 * Creates NERAnnotators to be used the the Ingesters.
 * Make configurable (command line?) See AnnotationManager_cmd.java
 * @author alexaulabaugh
 */

public class AnnotationManager
{
	private String classifierPath;
	private String posPath;
	private Gson gson;
	private EntityCatalog catalogTemplate;
	private GazetteerTable gzTable;
	private GazetteerTree gzTree;
	private List<TokenSequencePattern> patternList;
	private ArrayList<String> extractorSpecifications;
	private String reconcilerSpecification;
	private int windowSize;
	protected StanfordGazetteerPatternBuilder patternBuilder;
	
	public AnnotationManager(int windowSpecification)
	{
		try
		{
			extractorSpecifications = new ArrayList<String>();
			reconcilerSpecification = "";
			gson = new GsonBuilder().create();
			//Get local paths
			String path = new File(".").getAbsolutePath();
			String[] pathArr = path.split("/");
			path = String.join("/", Arrays.copyOfRange(pathArr, 0, pathArr.length-2));
			String localPathsFile = path + "/LOCALPATHS.txt";
			List<String> lines = Files.readAllLines(Paths.get(localPathsFile));
			for(int i = 0; i < lines.size(); i++)
			{
				String[] line = lines.get(i).split(":");
				if(line[0].equals("CLASSIFIER"))
					classifierPath = line[1];
				else if(line[0].equals("POSTAGGER"))
					posPath = line[1];
			}
			patternBuilder = new StanfordGazetteerPatternBuilder();
			windowSize = windowSpecification;
		}
		catch(Exception e)
		{
			e.printStackTrace();
		}
	}
	
	/**
	 * Builds the catalogTemplate, sets its Gazetteer structures
	 * Use GazetteerTree for GazetteerTreeExtractor, GazetteerTable for GazetteerTableExtractor,
	 * and setGazetteerList & patternBuilder for StanfordGazetteerExtractor
	 * Reads specifications from AnnotationConfig.txt
	 */
	public void setUpCatalog()
	{
		catalogTemplate = new EntityCatalog();
		gzTable = new GazetteerTable();
		gzTree = new GazetteerTree();
		try
		{
			List<String> configFileContents = Files.readAllLines(Paths.get("src/AnnotationConfig.txt"));
			for(String line : configFileContents)
			{
				if(line.contains("#"))
					continue;
				String[] components = line.split(":");
				switch(components[0])
				{
					case "gzMethod":
						setupGZMethod(components[1]);
						break;
					case "EntityOntology":
						addEntityTypesFromFile(catalogTemplate, components[1]);
						break;
					case "GazetteerFile":
						addGazetteerEntitiesFromJSON(components[1], catalogTemplate, Integer.MAX_VALUE);
						break;
					case "GazetteerFolder":
						addGazetteerEntitiesFromFolder(components[1], catalogTemplate);
						break;
					case "Extractor":
						extractorSpecifications.add(components[1]);
						if(components[1].equals("StanfordGazetteerExtractor"))
							patternBuilder.buildPatternsFromCatalog(catalogTemplate);
						break;
					case "Reconciler":
						reconcilerSpecification = components[1];
						break;
					default:
						System.out.println("Error: Unrecognized Line in Annotation Config:" + line);
				}
			}
		}
		catch (IOException e)
		{
			System.out.println("Error: Failed to load AnnotationManager Config File");
			e.printStackTrace();
		}
		
		addDefaultEntityTypes(catalogTemplate);
	}
	
	/**
	 * Returns a copy of the catalog
	 * @return
	 */
	public EntityCatalog getCatalogClone()
	{
		return catalogTemplate;
	}
	
	/**
	 * Sets up a specified gazetteer structure from the config file
	 * @param method
	 */
	private void setupGZMethod(String method)
	{
		switch(method)
		{
			case "GazetteerTable":
				catalogTemplate.setGazetteerTable(gzTable);
				break;
			case "GazetteerTree":
				catalogTemplate.setGazetteerTree(gzTree);
				break;
			case "GazetteerList":
				catalogTemplate.setGazetteerList(new ArrayList<EntityInstance>());
				break;
			default:
				System.out.println("Error: Unknown Gazetteer Method: " + method);
		}
	}
	
	/**
	 * Retrieves an annotator for an Ingestor. Builds from the catalogTemplate, and adds the
	 * individual extractors
	 * @param encoding the output format of the annotator
	 * @return
	 */
	public NERAnnotator getAnnotator(String encoding)
	{
		EntityCatalog myCatalog = catalogTemplate.cloneSelf();
		
		NERAnnotator myAnnotator = new NERAnnotator(myCatalog, encoding, windowSize);
		
		//initialize EntityExtractors
		
		for(String extractionMethod : extractorSpecifications)
		{
			switch(extractionMethod)
			{
				case "StanfordExtractor":
					myAnnotator.addExtractionTechnique(new StanfordExtractor(classifierPath, myAnnotator.getCatalog()));
					break;
				case "PhoneEmailExtractor":
					myAnnotator.addExtractionTechnique(new PhoneEmailExtractor(myAnnotator.getCatalog()));
					break;
				case "GolfExtractor":
					myAnnotator.addExtractionTechnique(new GolfExtractor(myAnnotator.getCatalog()));
					break;
				case "DBPediaExtractor":
					myAnnotator.addExtractionTechnique(new DBPediaExtractor(myAnnotator.getCatalog()));
					break;
				case "GazetteerTreeExtractor":
					myAnnotator.addExtractionTechnique(new GazetteerTreeExtractor(false, posPath, myAnnotator.getCatalog()));
					break;
				case "GazetteerTreeExtractorPOS":
					myAnnotator.addExtractionTechnique(new GazetteerTreeExtractor(true, posPath, myAnnotator.getCatalog()));
					break;
				case "GazetteerTableExtractor":
					myAnnotator.addExtractionTechnique(new GazetteerTableExtractor(myAnnotator.getCatalog()));
					break;
				case "StanfordGazetteerExtractor":
					StanfordGazetteerExtractor stanfordGzExtractor = new StanfordGazetteerExtractor(myAnnotator.getCatalog());
					stanfordGzExtractor.setPatternInstanceMap(patternBuilder.getPatternInstanceMap());
					stanfordGzExtractor.setPatternList(patternBuilder.getPatterns());
					stanfordGzExtractor.createMatcher();
					myAnnotator.addExtractionTechnique(stanfordGzExtractor);
					break;
				default:
					System.out.println("Error: Unrecognized Extractor: " + extractionMethod);
					break;
			}
		}
		
		//Reconciler
		
		switch(reconcilerSpecification)
		{
			case"RootsOnlyReconciler":
				myAnnotator.setAnnotationReconciler(new RootsOnlyReconciler());
				break;
			default:
				System.out.println("Error: Unrecognized Reconciler: " + reconcilerSpecification);
				break;
		}
		
		return myAnnotator;
	}
	
	/**
	 * Adds EntityTypes from a file to a catalog. Each EntityType is on a new line,
	 * in the format of "typename=supertype". The "=" must be present, even if there is no supertype.
	 * @param cat
	 * @param filename
	 */
	private void addEntityTypesFromFile(EntityCatalog cat, String filename)
	{
		//Citation: https://stackoverflow.com/questions/3806062/how-to-open-a-txt-file-and-read-numbers-in-java
		File entityFile = new File("src/entity_ontology/" + filename);
		try
		{
			BufferedReader reader = new BufferedReader(new FileReader(entityFile));
			String jsonStr = new String(Files.readAllBytes(Paths.get("src/entity_ontology/" + filename)));
			Map<String, List<String>> jsonObj = gson.fromJson(jsonStr, new TypeToken<Map<String, List<String>>>(){}.getType());
			for(String type : jsonObj.keySet())
			{
				if(!cat.containsEntityType(type))
					cat.addEntityType(new EntityType(type));
				EntityType entity = cat.getEntityType(type);
				for(String subtype : jsonObj.get(type))
				{
					if(!cat.containsEntityType(subtype))
						cat.addEntityType(new EntityType(subtype));
					cat.getEntityType(subtype).addSuperType(entity);
				}
			}
			reader.close();
		}
		catch(Exception e)
		{
			e.printStackTrace();
		}
	}
	
	/**
	 * Add some sample EntityTypes
	 * @param cat
	 */
	private void addDefaultEntityTypes(EntityCatalog cat)
	{
		//Define all entities
		EntityType number = new EntityType("NUMBER");
		EntityType money = new EntityType("MONEY");
		EntityType percent = new EntityType("PERCENT");
		EntityType date = new EntityType("DATE");
		EntityType time = new EntityType("TIME");
		EntityType phone = new EntityType("PHONE");
		EntityType email = new EntityType("EMAIL");
		EntityType state = new EntityType("STATE");
		
		//Add super-entities
		money.addSuperType(number);
		percent.addSuperType(number);
		date.addSuperType(number);
		time.addSuperType(number);
		
		cat.addEntityType(number);
		cat.addEntityType(money);
		cat.addEntityType(percent);
		cat.addEntityType(date);
		cat.addEntityType(time);
		cat.addEntityType(phone);
		cat.addEntityType(email);
		cat.addEntityType(state);
	}
	
	/**
	 * Adds all gazetteer files (JSON) in a folder
	 * @param filename
	 * @param catalog
	 */
	private void addGazetteerEntitiesFromFolder(String foldername, EntityCatalog catalog)
	{
		File gzDirectory = new File("src/entity_instances/" + foldername);
		for(String gzFilename : gzDirectory.list())
		{
			System.out.println(gzFilename);
			addGazetteerEntitiesFromJSON(foldername + "/" + gzFilename, catalog, Integer.MAX_VALUE);
		}
	}
	
	/**
	 * Reads a JSON file with EntityInstances.
	 * @param filename
	 * @param catalog
	 * @param maxInstances the maximum number of instances to record. (Used for testing. Use Integer.MAX_VALUE when not testing.)
	 */
	private void addGazetteerEntitiesFromJSON(String filename, EntityCatalog catalog, int maxInstances)
	{
		int numInstances = 0;
		int numIdentifiers = 0;
		int leftovers = 0;
		System.out.print("ADDING JSON GAZETTEER ENTRIES...");
		try
		{
			String jsonStr = new String(Files.readAllBytes(Paths.get("src/entity_instances/" + filename)));
			Map<String, List<List<String>>> jsonObj = gson.fromJson(jsonStr, new TypeToken<Map<String, List<List<String>>>>(){}.getType());
			int maxInstancesPerType = (int) Math.ceil(maxInstances/(jsonObj.keySet().size()*1.0));
			for(String type : jsonObj.keySet())
			{
				int numInstancesThisType = 0;
				for(List<String> inst : jsonObj.get(type))
				{
					if(numInstancesThisType >= (maxInstancesPerType+leftovers) || numInstances >= maxInstances)
						break;
					numInstances++;
					numInstancesThisType++;
					EntityInstance fileInstance = new EntityInstance(type + ":" + inst.get(0));
					fileInstance.addType(catalog.getEntityType(type));
					for(String synonym : inst)
					{
						fileInstance.addSynonym(synonym);
						numIdentifiers++;
					}
					GazetteerTable gzTable = catalog.getGazetteerTable();
					if(gzTable != null)
						gzTable.addInstance(fileInstance);
					GazetteerTree gzTree = catalog.getGazetteerTree();
					if(gzTree != null)
						gzTree.addInstance(fileInstance);
					catalog.addInstanceToList(fileInstance);
				}
				leftovers = Math.max(0, leftovers + maxInstancesPerType - numInstancesThisType);
			}
		}
		catch (IOException e)
		{
			e.printStackTrace();
		}
		System.out.println("DONE - ADDED " + numInstances + " INSTANCES WITH " + numIdentifiers + " IDENTIFIERS");
	}
	
	/**
	 * Adds gazetteer entities to the NERAnnotator.
	 * FOR TESTING PURPOSES ONLY
	 * @param catalog
	 */
	private void addGazetteerEntities(EntityCatalog catalog)
	{
		//addGazetteerEntitiesFromJSON("dbpediaInstancesSmall.json", catalog, Integer.MAX_VALUE);
		
		/*
		GazetteerTable gzTable = catalog.getGazetteerTable();
		GazetteerTree gzTree = catalog.getGazetteerTree();
		
		EntityInstance sampleInstance = new EntityInstance("State:Illinois");
		sampleInstance.addType(catalog.getEntityType("STATE"));
		sampleInstance.addSynonym("Illinois");
		sampleInstance.addSynonym("IL");
		if(gzTable != null)
			gzTable.addInstance(sampleInstance);
		if(gzTree != null)
			gzTree.addInstance(sampleInstance);
		catalog.addInstanceToList(sampleInstance);
		
		EntityInstance sampleInstance2 = new EntityInstance("Country:USA");
		sampleInstance2.addType(catalog.getEntityType("COUNTRY"));
		sampleInstance2.addSynonym("USA");
		sampleInstance2.addSynonym("U.S.A.");
		sampleInstance2.addSynonym("U.S.");
		sampleInstance2.addSynonym("United States");
		sampleInstance2.addSynonym("United States of America");
		sampleInstance2.addSynonym("America");
		if(gzTable != null)
			gzTable.addInstance(sampleInstance2);
		if(gzTree != null)
			gzTree.addInstance(sampleInstance2);
		catalog.addInstanceToList(sampleInstance2);
		
		EntityInstance sampleInstance3 = new EntityInstance("Song:IfICould");
		sampleInstance3.addType(catalog.getEntityType("SONG"));
		sampleInstance3.addSynonym("El Condor Pasa");
		sampleInstance3.addSynonym("If I Could");
		sampleInstance3.addSynonym("El Condor Pasa / If I Could");
		if(gzTable != null)
			gzTable.addInstance(sampleInstance3);
		if(gzTree != null)
			gzTree.addInstance(sampleInstance3);
		catalog.addInstanceToList(sampleInstance3);
		*/
		
	}
}
