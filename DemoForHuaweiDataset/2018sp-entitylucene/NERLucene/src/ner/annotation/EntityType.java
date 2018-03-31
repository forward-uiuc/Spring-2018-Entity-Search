package ner.annotation;

import java.util.ArrayList;

/**
 * Class representing the type of an entity.
 * Entities can be of multiple types, and an EntityType can have supertypes
 * e.g. A "MUSICIAN" isa "PERSON" and isa "PROFESSION"
 * @author aaulabaugh@gmail.com
 */

public class EntityType implements java.io.Serializable
{
	/**
	 * default serial version UID
	 */
	private static final long serialVersionUID = 1L;

	//The identifying string of the entity type e.g. "PERSON"
	private String id;
	
	//All super entity types
	private ArrayList<EntityType> superTypes;
	
	public EntityType()
	{
		id = null;
		superTypes = new ArrayList<EntityType>();
	}
	
	public EntityType clone()
	{
		EntityType myClone = new EntityType();
		myClone.id = this.id;
		myClone.superTypes = new ArrayList<EntityType>();
		for(EntityType type : this.superTypes)
			myClone.superTypes.add(type.clone());
		return myClone;
	}
	
	public EntityType(String id_set)
	{
		id = id_set.toLowerCase();
		superTypes = new ArrayList<EntityType>();
	}
	
	public void setID(String id_setter)
	{
		id = id_setter.toLowerCase();
	}
	
	public void addSuperType(EntityType type)
	{
		superTypes.add(type);
	}
	
	public String getID()
	{
		return id;
	}
	
	public ArrayList<EntityType> getSuperTypes()
	{
		return superTypes;
	}
	
	/**
	 * @return The number of super entity types in the entire entity type tree
	 */
	public int numberOfParents()
	{
		int parentCount = 0;
		for(EntityType t : superTypes)
		{
			parentCount += 1 + t.numberOfParents();
		}
		return parentCount;
	}
}
