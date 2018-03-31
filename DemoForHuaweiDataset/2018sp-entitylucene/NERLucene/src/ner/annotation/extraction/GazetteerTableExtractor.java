package ner.annotation.extraction;

import java.util.ArrayList;

import ner.annotation.EntityAnnotation;
import ner.annotation.EntityCatalog;
import ner.annotation.EntityInstance;
import ner.annotation.EntityType;

public class GazetteerTableExtractor extends EntityExtractor
{

	public GazetteerTableExtractor(EntityCatalog cat)
	{
		super(cat);
	}
	
	private void gazetteerLookup(EntityAnnotation annotationToCheck)
	{
		for(EntityAnnotation subAnnotation : annotationToCheck.getChildren())
		{
			gazetteerLookup(subAnnotation);
		}
		for(EntityType type : annotationToCheck.getTypes())
		{
			ArrayList<EntityInstance> matches = catalog.getGazetteerTable().getEntityInstances(type, annotationToCheck.getContent());
			if(matches == null)
				continue;
			for(EntityInstance match : matches)
			{
				annotationToCheck.setContent(match.getIdentifier());
				annotationToCheck.setSource(annotationToCheck.getSource() + ",Gazetteer");
				annotationToCheck.setTypes(match.getTypes());
			}
		}
		/*
		ArrayList<EntityInstance> subGazetteer = new ArrayList<EntityInstance>();
		for(EntityType type : annotationToCheck.getTypes())
		{
			ArrayList<EntityInstance> thisTypeList = catalog.getEntityInstances(type);
			if(thisTypeList != null)
			{
				subGazetteer.addAll(thisTypeList);
			}
		}
		for(EntityInstance inst : subGazetteer)
		{
			if(inst.isSynonym(annotationToCheck.getContent()))
			{
				annotationToCheck.setContent(inst.getIdentifier());
				annotationToCheck.setSource(annotationToCheck.getSource() + ",Gazetteer");
				annotationToCheck.setTypes(inst.getTypes());
			}
		}
		for(EntityAnnotation subAnnotation : annotationToCheck.getChildren())
		{
			gazetteerLookup(subAnnotation);
		}
		*/
	}

	@Override
	public void extractEntities(String text)
	{
		for(EntityAnnotation annotationToCheck : catalog.getAnnotaitons())
		{
			gazetteerLookup(annotationToCheck);
		}
	}
}
