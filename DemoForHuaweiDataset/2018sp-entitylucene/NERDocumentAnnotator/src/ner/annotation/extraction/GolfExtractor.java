package ner.annotation.extraction;

import java.io.IOException;
import java.io.StringReader;

import ner.annotation.EntityAnnotation;
import ner.annotation.EntityCatalog;

public class GolfExtractor extends EntityExtractor
{
	private GolfExtractorImpl scanner;
	
	public GolfExtractor(EntityCatalog cat)
	{
		super(cat);
		scanner = new GolfExtractorImpl(new StringReader(""));
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
