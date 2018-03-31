package ner.annotation.extraction;

import java.io.IOException;
import java.util.ArrayList;

import ner.annotation.EntityAnnotation;
import ner.annotation.EntityCatalog;
import ner.annotation.NERAnnotator;

/**
 * Abstract class for all entity extraction techniques
 * @author aaulabaugh@gmail.com
 */
public abstract class EntityExtractor
{
	
	protected EntityCatalog catalog;
	
	public EntityExtractor(EntityCatalog cat)
	{
		catalog = cat;
	}
	
	/**
	 * Used for any final print statements after annotation finishes.
	 */
	public void close()
	{
		//do nothing by default
	}
	
	/**
	 * Given input text, generate an array of EntityAnnotations.
	 * @param text
	 * @return
	 */
	public abstract void extractEntities(String text);
}
