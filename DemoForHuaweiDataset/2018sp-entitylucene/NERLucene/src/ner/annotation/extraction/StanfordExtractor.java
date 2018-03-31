package ner.annotation.extraction;

import java.util.ArrayList;
import java.util.List;

import edu.stanford.nlp.ie.AbstractSequenceClassifier;
import edu.stanford.nlp.ie.crf.CRFClassifier;
import edu.stanford.nlp.ling.CoreLabel;
import edu.stanford.nlp.ling.CoreAnnotations.AnswerAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations.CharacterOffsetBeginAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations.CharacterOffsetEndAnnotation;
import ner.annotation.EntityAnnotation;
import ner.annotation.EntityCatalog;
import ner.annotation.NERAnnotator;

/**
 * EntityExtractor for StanfordNER techniques
 * @author alexaulabaugh
 */

public class StanfordExtractor extends EntityExtractor
{
	private int foundEntities;
	private AbstractSequenceClassifier<CoreLabel> stanfordClassifier;
	
	/**
	 * Initializes the Stanford classifier
	 * @param classifierPath the local path to the compressed classifier
	 * @param cat
	 */
	public StanfordExtractor(String classifierPath, EntityCatalog cat)
	{
		super(cat);
		foundEntities = 0;
		try
		{
			stanfordClassifier = CRFClassifier.getClassifier(classifierPath);
		}
		catch(Exception e)
		{
			e.printStackTrace();
		}
	}
	
	/**
	 * Reconciles the type naming with that of DBPedia
	 * @param type
	 * @return
	 */
	private String disambiguateType(String type)
	{
		if(type.toLowerCase().equals("organization"))
		{
			return "organisation";
		}
		if(type.toLowerCase().equals("location"))
		{
			return "place";
		}
		return type;
	}
	
	@Override
	public void extractEntities(String text)
	{
		try
		{
			List<List<CoreLabel>> labels = stanfordClassifier.classify(text);
			EntityAnnotation newAnnotation = null;
			String stanfordAnnotatedType = "";
			for (List<CoreLabel> sentences : labels)
			{
				for (CoreLabel label : sentences)
				{
					String type = disambiguateType(label.getString(AnswerAnnotation.class).toLowerCase());
					if(newAnnotation != null)
					{
						if (type.equals(stanfordAnnotatedType) && !type.equals("o"))
						{
							newAnnotation.setContent(text.substring(newAnnotation.getPosition(), label.get(CharacterOffsetEndAnnotation.class)));
						}
						else
						{
							//System.out.println("Found " + newAnnotation.getType() + ": " + newAnnotation.getContent());
							if(!stanfordAnnotatedType.equals("o"))
							{
								foundEntities++;
								newAnnotation.addType(catalog.getEntityType(stanfordAnnotatedType));
							}
							catalog.addAnnotation(newAnnotation);
							newAnnotation = new EntityAnnotation();
							newAnnotation.setPosition(label.get(CharacterOffsetBeginAnnotation.class));
							newAnnotation.setContent(text.substring(newAnnotation.getPosition(), label.get(CharacterOffsetEndAnnotation.class)));
							stanfordAnnotatedType = disambiguateType(type);
							newAnnotation.setSource("STANFORD");
						}
					}
					else
					{
						newAnnotation = new EntityAnnotation();
						newAnnotation.setPosition(label.get(CharacterOffsetBeginAnnotation.class));
						newAnnotation.setContent(text.substring(newAnnotation.getPosition(), label.get(CharacterOffsetEndAnnotation.class)));
						stanfordAnnotatedType = disambiguateType(type);
						newAnnotation.setSource("STANFORD");
					}
				}
			}
			if(newAnnotation != null)
			{
				if(!stanfordAnnotatedType.equals("o"))
					newAnnotation.addType(catalog.getEntityType(stanfordAnnotatedType));
				catalog.addAnnotation(newAnnotation);
			}
		}
		catch(Exception e)
		{
			System.out.println("Stanford Classifier Error");
			e.printStackTrace();
		}
	}
	@Override
	public void close()
	{
		System.out.println("StanfordExtractor: FOUND " + foundEntities + " ENTITIES");
	}
}
