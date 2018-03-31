package ner.annotation.extraction;

import java.io.StringReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.regex.Pattern;

import edu.stanford.nlp.ie.AbstractSequenceClassifier;
import edu.stanford.nlp.ie.crf.CRFClassifier;
import edu.stanford.nlp.ling.CoreAnnotations.CharacterOffsetBeginAnnotation;
import edu.stanford.nlp.ling.CoreLabel;
import edu.stanford.nlp.ling.Word;
import edu.stanford.nlp.ling.tokensregex.Env;
import edu.stanford.nlp.ling.tokensregex.MatchedExpression;
import edu.stanford.nlp.ling.tokensregex.MultiPatternMatcher;
import edu.stanford.nlp.ling.tokensregex.SequenceMatchResult;
import edu.stanford.nlp.ling.tokensregex.SequencePattern;
import edu.stanford.nlp.ling.tokensregex.TokenSequencePattern;
import edu.stanford.nlp.process.CoreLabelTokenFactory;
import edu.stanford.nlp.process.PTBTokenizer;
import edu.stanford.nlp.process.TokenizerFactory;
import edu.stanford.nlp.util.CoreMap;
import ner.annotation.EntityAnnotation;
import ner.annotation.EntityCatalog;
import ner.annotation.EntityInstance;
import ner.annotation.EntityType;

/**
 * Uses Stanford TokenRegex to do Gazetteer Extraction
 * See https://nlp.stanford.edu/software/tokensregex.html
 * @author alexaulabaugh
 */

public class StanfordGazetteerExtractor extends EntityExtractor
{
	private MultiPatternMatcher stanfordMatcher;
	//private AbstractSequenceClassifier<CoreLabel> stanfordClassifier;
	private TokenizerFactory<CoreLabel> tokenizerFactory;
	private List<TokenSequencePattern> patternList;
	private HashMap<String, ArrayList<EntityInstance>> knownPatternInstances;
	private Env env;
	private int foundEntities;
	
	public StanfordGazetteerExtractor(EntityCatalog cat)
	{
		super(cat);
		foundEntities = 0;
		try
		{
			knownPatternInstances = new HashMap<String, ArrayList<EntityInstance>>();
			//stanfordClassifier = CRFClassifier.getClassifier(classifierPath);
			tokenizerFactory = PTBTokenizer.factory(new CoreLabelTokenFactory(), "");
		}
		catch(Exception e)
		{
			e.printStackTrace();
		}
	}
	
	public void setPatternList(List<TokenSequencePattern> patterns)
	{
		patternList = patterns;
	}
	
	public void setPatternInstanceMap(HashMap<String, ArrayList<EntityInstance>> map)
	{
		knownPatternInstances = map;
	}
	
	public void createMatcher()
	{
		stanfordMatcher = TokenSequencePattern.getMultiPatternMatcher(patternList);
	}

	@Override
	public void extractEntities(String text)
	{
		//System.out.println(text);
		//System.out.println();
		try
		{
			List<CoreLabel> tokens = tokenizerFactory.getTokenizer(new StringReader(text)).tokenize();
			List<SequenceMatchResult<CoreMap>> result = stanfordMatcher.findNonOverlapping(tokens);
			Iterator<SequenceMatchResult<CoreMap>> matchIterator = result.iterator();
			while(matchIterator.hasNext())
			{
				ArrayList<String> matchedStringComponents = new ArrayList<String>();
				SequenceMatchResult<CoreMap> match = matchIterator.next();
				CoreMap matchBegin = match.elements().get(match.start());
				int beginOffset = matchBegin.get(CharacterOffsetBeginAnnotation.class);
				SequencePattern<CoreMap> matchedPattern = match.pattern();
				ArrayList<EntityInstance> matchedInstances = knownPatternInstances.get(matchedPattern.toString());
				for(int i = match.start(); i < match.end(); i++)
				{
					matchedStringComponents.add(match.elements().get(i).toString());
				}
				String matchedString = String.join(" ", matchedStringComponents);
				for(EntityInstance inst : matchedInstances)
				{
					foundEntities += 1;
					EntityAnnotation newAnnotation = new EntityAnnotation();
					newAnnotation.setPosition(beginOffset);
					newAnnotation.setContent(matchedString);
					newAnnotation.setSource("StanfordGazetteer");
					for(EntityType type : inst.getTypes())
						newAnnotation.addType(type);
					catalog.addAnnotation(newAnnotation);
					//System.out.println(newAnnotation);
				}
			}
			//System.out.println();
		}
		catch(Exception e)
		{
			e.printStackTrace();
		}
	}
	
	@Override
	public void close()
	{
		System.out.println("GazetteerTreeExtractor: FOUND " + foundEntities + " ENTITIES");
	}

}
