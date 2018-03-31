package ner.annotation.treegazetteer;

import java.util.ArrayList;
import ner.annotation.EntityInstance;

public class GazetteerTraverser
{
	private GazetteerNode currentNode;
	private String token;
	private int tokenLength;
	
	public GazetteerTraverser(GazetteerNode head)
	{
		currentNode = head;
		tokenLength = 0;
		token = "";
	}
	
	public boolean nextChar(char c)
	{
		currentNode = currentNode.getChild(c);
		tokenLength++;
		token += c;
		return currentNode != null;
	}
	
	public int getTokenLength()
	{
		return tokenLength;
	}
	
	public String getToken()
	{
		return token;
	}
	
	public ArrayList<EntityInstance> getInstances()
	{
		if(currentNode != null)
			return currentNode.getInstances();
		else
			return null;
	}
}
