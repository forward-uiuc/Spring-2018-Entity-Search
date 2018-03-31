package ner.annotation.extraction;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.regex.Pattern;

import edu.stanford.nlp.ling.tokensregex.Env;
import edu.stanford.nlp.ling.tokensregex.TokenSequencePattern;
import ner.annotation.EntityCatalog;
import ner.annotation.EntityInstance;

/**
 * Class for building RegEx out of EntityInstances for Stanford's TokenRegex.
 * @author alexaulabaugh
 */

public class StanfordGazetteerPatternBuilder
{
	private EntityCatalog catalog;
	//Mapping from RegEx to EntityInstances
	private HashMap<String, ArrayList<EntityInstance>> knownPatternInstances;
	//All RegEx for EntityInstances
	private List<TokenSequencePattern> patternList;
	private Env env;
	
	public StanfordGazetteerPatternBuilder()
	{
		env = TokenSequencePattern.getNewEnv();
		env.setDefaultStringPatternFlags(Pattern.CASE_INSENSITIVE);
		env.setDefaultStringMatchFlags(Pattern.CASE_INSENSITIVE);
		knownPatternInstances = new HashMap<String, ArrayList<EntityInstance>>();
		patternList = new ArrayList<TokenSequencePattern>();
	}
	
	/**
	 * Uses all EntityInstances in supplied Catalog, then clears them as they are no longer needed in the Catalog
	 * @param cat
	 */
	public void buildPatternsFromCatalog(EntityCatalog cat)
	{
		catalog = cat;
		System.out.println("Adding instances to Pattern Matcher...");
		int i = 0;
		for(EntityInstance inst : cat.getGazetteerList())
		{
			i++;
			addInstance(inst);
			if(i % 100000 == 0)
			{
				System.out.println(i);
			}
		}
		System.out.println("Done");
		cat.setGazetteerList(null);
	}
	
	/**
	 * Generates RegEx for a single EntityInstance
	 * @param inst
	 */
	private void addInstance(EntityInstance inst)
	{
		ArrayList<String> instancePatternStrings = new ArrayList<String>();
		for(String syn : inst.getSynonyms())
		{
			instancePatternStrings.add("(\"" + String.join("\" \"", syn.split(" ")) + "\")");
		}
		String instancePatternString = String.join("|", instancePatternStrings);
		//System.out.println(instancePatternString);
		if(knownPatternInstances.containsKey(instancePatternString))
		{
			knownPatternInstances.get(instancePatternString).add(inst);
		}
		else
		{
			ArrayList<EntityInstance> newInstanceList = new ArrayList<EntityInstance>();
			newInstanceList.add(inst);
			knownPatternInstances.put(instancePatternString, newInstanceList);
		}
		try
		{
			patternList.add(TokenSequencePattern.compile(env, instancePatternString));
		}
		catch(Exception e)
		{
			e.printStackTrace();
		}
	}
	
	/**
	 * Gets all RegEx expressions
	 * @return
	 */
	public List<TokenSequencePattern> getPatterns()
	{
		return patternList;
	}
	
	/**
	 * Gets the mapping from the RegEx to the EntityInstances
	 * @return
	 */
	public HashMap<String, ArrayList<EntityInstance>> getPatternInstanceMap()
	{
		return knownPatternInstances;
	}
}
