package ner.annotation.treegazetteer;

import java.util.ArrayList;
import java.util.HashMap;

import ner.annotation.EntityInstance;

public class GazetteerNode
{
	private char identifier;
	private HashMap<Character, GazetteerNode> children;
	private ArrayList<EntityInstance> instances;
	
	public GazetteerNode(char id)
	{
		identifier = id;
		children = new HashMap<Character, GazetteerNode>();
		instances = null;
	}
	
	public char getID()
	{
		return identifier;
	}
	
	public ArrayList<EntityInstance> getInstances()
	{
		return instances;
	}
	
	public void addChild(GazetteerNode child)
	{
		children.put(child.getID(), child);
	}
	
	public void addInstance(EntityInstance inst)
	{
		if(instances == null)
			instances = new ArrayList<EntityInstance>();
		instances.add(inst);
	}
	
	public GazetteerNode getChild(char c)
	{
		if(children.containsKey(c))
			return children.get(c);
		return null;
	}
}
