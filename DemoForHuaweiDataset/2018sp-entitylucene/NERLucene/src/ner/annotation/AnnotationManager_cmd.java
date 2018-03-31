package ner.annotation;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Hashtable;
import java.util.List;

import ner.annotation.extraction.DBPediaExtractor;
import ner.annotation.extraction.GazetteerTableExtractor;
import ner.annotation.extraction.PhoneEmailExtractor;
import ner.annotation.extraction.StanfordExtractor;

/**
 * Creates NERAnnotators to be used the the Ingesters.
 * TODO Make configurable (command line?)
 * @author jaewookim, alexaulabaugh
 */

public class AnnotationManager_cmd
{
	private String classifierPath;
	private EntityCatalog myCatalog;
	private int windowSize;
	
	public AnnotationManager_cmd()
	{
		try
		{
			//Get local path
			String path = new File(".").getAbsolutePath();
			String[] pathArr = path.split("/");
			path = String.join("/", Arrays.copyOfRange(pathArr, 0, pathArr.length-2));
			String localPathsFile = path + "/LOCALPATHS.txt";
			List<String> lines = Files.readAllLines(Paths.get(localPathsFile));
			classifierPath = lines.get(3).split(":")[1];
			myCatalog = new EntityCatalog();
			GazetteerTable gzTable = new GazetteerTable();
			myCatalog.setGazetteerTable(gzTable);
			windowSize = 100;
		}
		catch(Exception e)
		{
			e.printStackTrace();
		}
	}
	
	public NERAnnotator getAnnotator()
	{
//		addEntityTypes(cat);
//		addGazetteerEntities(cat);

		addEntityTypesFromFile(myCatalog, "dbpedia_ontology.txt");
		addGazetteerEntities(myCatalog);

		
		NERAnnotator myAnnotator = new NERAnnotator(myCatalog.cloneSelf(), "JSON", windowSize);
		
		//initialize EntityExtractors
		myAnnotator.addExtractionTechnique(new DBPediaExtractor(myAnnotator.getCatalog()));
		myAnnotator.addExtractionTechnique(new PhoneEmailExtractor(myAnnotator.getCatalog()));
		myAnnotator.addExtractionTechnique(new StanfordExtractor(classifierPath, myAnnotator.getCatalog()));
		myAnnotator.addExtractionTechnique(new GazetteerTableExtractor(myAnnotator.getCatalog()));
		myAnnotator.setAnnotationReconciler(new RootsOnlyReconciler());
		
		return myAnnotator;
	}
	
	private static void addEntityTypesFromFile(EntityCatalog cat, String filename)
	{
		//Citation: https://stackoverflow.com/questions/3806062/how-to-open-a-txt-file-and-read-numbers-in-java
		File entityFile = new File("src/entity_specifications/" + filename);
		try
		{
			BufferedReader reader = new BufferedReader(new FileReader(entityFile));
			String line = "";
			Hashtable<String, String[]> supertypeMap = new Hashtable<String, String[]>();
			while ((line = reader.readLine()) != null)
			{
				String[] components = line.split("=");
				String name = components[0];
				cat.addEntityType(new EntityType(name));
				if(components.length > 1)
					supertypeMap.put(name, components[1].split(";"));
			}
			reader.close();
			for(String name : supertypeMap.keySet())
			{
				EntityType entity = cat.getEntityType(name);
				for(String supertype : supertypeMap.get(name))
				{
					entity.addSuperType(cat.getEntityType(supertype));
				}
			}
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
	}
	
	/**
	 * Add some EntityTypes
	 * @param cat
	 */
	public void addEntityType_interface(String Entity)
	{
		//Define all entities
//		EntityType person = new EntityType("PERSON");
//		EntityType location = new EntityType("LOCATION");
//		EntityType organization = new EntityType("ORGANIZATION");
//		EntityType phone = new EntityType("PHONE");
//		EntityType email = new EntityType("EMAIL");
//		EntityType state = new EntityType("STATE");
//		EntityType country = new EntityType("COUNTRY");
//		
//		//Add super-entities
//		state.addSuperType(location);
//		country.addSuperType(location);
//		
//		
//		cat.addEntityType(person);
//		cat.addEntityType(location);
//		cat.addEntityType(organization);
//		cat.addEntityType(phone);
//		cat.addEntityType(email);
//		cat.addEntityType(state);
//		cat.addEntityType(country);
		
		EntityType person = new EntityType(Entity);
		myCatalog.addEntityType(person);
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
	public ArrayList<String> getEntityTypes(){
		return myCatalog.getEntityTypes();
	}
}
