package ner.annotation.extraction;

import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;

import ner.annotation.EntityAnnotation;
import ner.annotation.EntityCatalog;
import ner.annotation.NERAnnotator;

/**
 * Wraps the jflex generated scanner to get all the entities at once
 * @author aaulabaugh@gmail.com
 */

public class PhoneEmailExtractor extends EntityExtractor
{
	private PhoneEmailExtractorImpl scanner;
		
	/**
	 * Initializes the jflex scanner
	 */
	public PhoneEmailExtractor(EntityCatalog cat)
	{
		super(cat);
		scanner = new PhoneEmailExtractorImpl(new StringReader(""));
		scanner.setCatalog(cat);
	}
	
	@Override
	public void extractEntities(String text)
	{
		scanner.yyreset(new StringReader(text));
		EntityAnnotation entity;
		try
		{
			while ((entity = scanner.getNextToken())!=null)
				catalog.addAnnotation(entity);
		}
		catch(IOException e)
		{
			e.printStackTrace();
		}
	}

}
