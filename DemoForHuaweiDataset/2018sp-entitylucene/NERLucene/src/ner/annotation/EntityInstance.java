package ner.annotation;

import java.util.ArrayList;

/**
 * Represents an entity which is known prior to NER.
 * Used for gazetteer methods.
 * @author aaulabaugh@gmail.com
 */

public class EntityInstance
{
	//The ID used to refer to this EntityInstance
	private String identifier;
	
	//The entity category (e.g. LOCATION)
	private ArrayList<EntityType> entityTypes;
	
	//List of text phrases which reference this entity
	private ArrayList<String> synonyms;
			
	public EntityInstance(String id)
	{
		identifier = id;
		synonyms = new ArrayList<String>();
		entityTypes = new ArrayList<EntityType>();
	}
	
	public void addType(EntityType type)
	{
		entityTypes.add(type);
	}
	
	public void addSynonym(String synonym)
	{
		if(synonym.equals(synonym.toUpperCase()))
			synonyms.add(synonym);
		else
			synonyms.add(synonym.toLowerCase());
	}
	
	public String getIdentifier()
	{
		return identifier;
	}
	
	public ArrayList<EntityType> getTypes()
	{
		return entityTypes;
	}
	
	public ArrayList<String> getSynonyms()
	{
		return synonyms;
	}
	
	public boolean isSynonym(String toCheck)
	{
		if(toCheck.equals(toCheck.toUpperCase()))
			return synonyms.contains(toCheck);
		return synonyms.contains(toCheck.toLowerCase());
	}
}
