package ner.annotation.treegazetteer;

import ner.annotation.EntityInstance;

public class GazetteerTree
{
	private GazetteerNode head;
	
	public GazetteerTree()
	{
		head = new GazetteerNode('\n');
	}
	
	public void addInstance(EntityInstance inst)
	{
		for(String synonym : inst.getSynonyms())
		{
			GazetteerNode currentNode = head;
			char[] synChars = synonym.toCharArray();
			for(int i = 0; i < synChars.length; i++)
			{
				GazetteerNode nextNode = currentNode.getChild(synChars[i]);
				if(nextNode == null)
				{
					nextNode = new GazetteerNode(synChars[i]);
					currentNode.addChild(nextNode);
				}
				currentNode = nextNode;
			}
			currentNode.addInstance(inst);
		}
	}
	
	public GazetteerNode getHead()
	{
		return head;
	}
}
