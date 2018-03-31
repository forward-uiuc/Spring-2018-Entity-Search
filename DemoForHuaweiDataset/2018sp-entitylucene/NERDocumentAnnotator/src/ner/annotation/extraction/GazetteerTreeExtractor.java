package ner.annotation.extraction;

import java.io.StringReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import ner.annotation.EntityAnnotation;
import ner.annotation.EntityCatalog;
import ner.annotation.EntityInstance;
import ner.annotation.EntityType;
import ner.annotation.treegazetteer.GazetteerTraverser;
import ner.annotation.treegazetteer.GazetteerTree;

import edu.stanford.nlp.ling.SentenceUtils;
import edu.stanford.nlp.ling.TaggedWord;
import edu.stanford.nlp.ling.HasWord;
import edu.stanford.nlp.tagger.maxent.MaxentTagger;

/**
 * A dictionary-based NER technique which leverages a simple character tree (GazetteerTree).
 * @author alexaulabaugh
 */

public class GazetteerTreeExtractor extends EntityExtractor
{
	
	//The token which, when seen in the input, will spawn a new GazetteerTraverser at that point
	private final char DEFAULT_DELIMITER = ' ';
	//The tokens which indicate the end of a token - one of these must be at the end of every annotation.
	private final Set<Character> TOKEN_TERMINATORS = new HashSet<Character>(Arrays.asList(new Character[] {' ', '.', ',', '!', '?', ';', ':', '(', ')', '-', '_', '\n', '\t'}));
	//From Stanford's POS tagger, these are the nouns. All entities are assumed to be nouns.
	private final Set<String> NOUN_TAGS = new HashSet<String>(Arrays.asList(new String[] {"NN", "NNS", "NNP", "NNPS"}));
	//The minimum token length of a matched entity
	private final int DEFAULT_MIN_TOKEN_LENGTH = 0;
	//All currently active GazetteerTraversers, which may be at different points in the GazetteerTree
	private ArrayList<GazetteerTraverser> traversers;
	
	private GazetteerTree tree;
	private char tokenDelimiter;
	private int minTokenLength;
	private int entitiesFound;
	
	//Tags the input with part of speech to improve accuracy.
	private MaxentTagger posTagger;

	public GazetteerTreeExtractor(boolean usePOS, String posPath, EntityCatalog cat)
	{
		super(cat);
		traversers = new ArrayList<GazetteerTraverser>();
		tree = cat.getGazetteerTree();
		tokenDelimiter = DEFAULT_DELIMITER;
		minTokenLength = DEFAULT_MIN_TOKEN_LENGTH;
		entitiesFound = 0;
		posTagger = null;
		if(usePOS)
			posTagger = new MaxentTagger(posPath);
	}
	
	public void setDelimiter(char delim)
	{
		tokenDelimiter = delim;
	}
	
	public void setMinTokenLength(int len)
	{
		minTokenLength = len;
	}
	
	public int getEntitiesFound()
	{
		return entitiesFound;
	}

	@Override
	public void extractEntities(String text)
	{
		String nounOnlyText = null;
		//nounOffsets.clear();
		if(posTagger != null)
		{
			nounOnlyText = "";
			List<List<HasWord>> sentences = MaxentTagger.tokenizeText(new StringReader(text));
			for (List<HasWord> sentence : sentences)
			{
			      List<TaggedWord> tSentence = posTagger.tagSentence(sentence);
			      for(TaggedWord word : tSentence)
			      {
			    	  if(NOUN_TAGS.contains(word.tag()))
			    	  {
			    		  nounOnlyText += word.word() + " ";
			    	  }  
			      }
			}
		}
		//System.out.println();
		if(nounOnlyText != null)
			text = nounOnlyText;
		if(tree == null)
			return;
		int offset = 0;
		traversers.add(new GazetteerTraverser(tree.getHead()));
		char[] inputArray = text.toCharArray();
		String currentTok = "";
		boolean foundEntity = false;
		for(int i = 0; i <= inputArray.length; i++)
		{
			char currentChar = '\n';
			char nextChar = '\n';
			if(i == inputArray.length)
				currentChar = tokenDelimiter;
			else
				currentChar = inputArray[i];
			if(i+1 < inputArray.length)
				nextChar = inputArray[i+1];
			//System.out.print(currentChar);
			for(Iterator<GazetteerTraverser> iterator = traversers.iterator(); iterator.hasNext();)
			{
				GazetteerTraverser traverser = iterator.next();
				if(traverser.nextChar(Character.toLowerCase(currentChar)) == false)
					iterator.remove();
				else
				{
					ArrayList<EntityInstance> instances = traverser.getInstances();
					if(instances == null || traverser.getTokenLength() < minTokenLength || !TOKEN_TERMINATORS.contains(nextChar))
						continue;		
					for(EntityInstance foundInstance : instances)
					{
						//if(!foundInstance.isSynonym(currentTok + currentChar))
						//	continue;
						foundEntity = true;
						entitiesFound++;
						EntityAnnotation gazetteerAnnotation = new EntityAnnotation();
						gazetteerAnnotation.setContent(traverser.getToken());
						gazetteerAnnotation.setPosition(offset - (traverser.getTokenLength()-1));
						gazetteerAnnotation.setSource("GAZETTEERTABLE");
						for(EntityType type : foundInstance.getTypes())
							gazetteerAnnotation.addType(type);
						//System.out.print("[" + gazetteerAnnotation.getTypes().get(0).getID() + ":" + gazetteerAnnotation.getContent() + "]");
						catalog.addAnnotation(gazetteerAnnotation);
					}
					
				}
			}
			if(currentChar == tokenDelimiter)
			{
				if(foundEntity == false)
				{
					EntityAnnotation tokenAnnotation = new EntityAnnotation();
					tokenAnnotation.setContent(currentTok);
					tokenAnnotation.setPosition(offset - (currentTok.length()-1));
					tokenAnnotation.setSource("GAZETTEERTABLE_TOKENIZATION");
					catalog.addAnnotation(tokenAnnotation);
					//System.out.print("[" + tokenAnnotation.getContent() + "]");
					currentTok = "";
				}
				else
				{
					foundEntity = false;
					currentTok = "";
				}
				traversers.add(new GazetteerTraverser(tree.getHead()));
			}
			else if(Character.isAlphabetic(currentChar))
				currentTok += currentChar;
			offset++;
		}
		//System.out.println();
	}
	
	@Override
	public void close()
	{
		System.out.println("GazetteerTreeExtractor: FOUND " + entitiesFound + " ENTITIES");
	}
}
