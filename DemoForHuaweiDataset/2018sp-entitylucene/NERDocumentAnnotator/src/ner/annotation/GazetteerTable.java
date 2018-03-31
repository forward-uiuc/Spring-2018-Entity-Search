package ner.annotation;

import java.util.ArrayList;
import java.util.HashMap;

/**
 * A class for holding EntityInstances across catalogs
 * @author alexaulabaugh
 */

public class GazetteerTable
{
	protected HashMap<EntityType, HashMap<String, ArrayList<EntityInstance>>> knownEntities;
	
	public GazetteerTable()
	{
		reset();
	}
	
	/**
	 * Clears the knownEntities
	 */
	public void reset()
	{
		knownEntities = new HashMap<EntityType, HashMap<String, ArrayList<EntityInstance>>>();
	}
	
	public HashMap<EntityType, HashMap<String, ArrayList<EntityInstance>>> getKnownEntities()
	{
		return knownEntities;
	}
	
	/**
	 * (Recursive helper method for addKnownEntity)
	 * Recursively adds this EntityInstance into the knownEntries dictionary with this type
	 * and all of its parent types
	 * @param newEntityInstance
	 * @param type
	 */
	private void addEntityInstanceWithType(EntityInstance newEntityInstance, EntityType type)
	{
		for(EntityType parentType : type.getSuperTypes())
		{
			addEntityInstanceWithType(newEntityInstance, parentType);
		}
		if(!knownEntities.containsKey(type))
		{
			knownEntities.put(type, new HashMap<String, ArrayList<EntityInstance>>());
		}
		for(String synonym : newEntityInstance.getSynonyms())
		{
			if(!knownEntities.get(type).containsKey(synonym))
			{
				knownEntities.get(type).put(synonym, new ArrayList<EntityInstance>());
			}
			knownEntities.get(type).get(synonym).add(newEntityInstance);
		}
	}
	
	/**
	 * Adds a new entry to the known entities for gazetteer methods
	 * @param newEntityInstance an EntityInstance which defines a known entity's name and synonyms
	 */
	public void addInstance(EntityInstance newEntityInstance)
	{
		for(EntityType type : newEntityInstance.getTypes())
		{
			addEntityInstanceWithType(newEntityInstance, type);
		}
	}
	
	/**
	 * Method to access the gazette
	 * @param type the EntityType to get a list for
	 * @return a list of all EntityInstances matching this type
	 */
	public ArrayList<EntityInstance> getEntityInstances(EntityType type, String tokenText)
	{
		HashMap<String, ArrayList<EntityInstance>> typeMap = knownEntities.get(type);
		if(typeMap != null)
			return typeMap.get(tokenText.toLowerCase());
		return null;
		
	}
}
