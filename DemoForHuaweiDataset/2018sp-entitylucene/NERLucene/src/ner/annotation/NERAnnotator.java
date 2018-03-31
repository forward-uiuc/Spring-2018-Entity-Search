package ner.annotation;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.Reader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import com.google.gson.*;

import com.sun.org.apache.xerces.internal.impl.dv.util.Base64;

import ner.annotation.extraction.EntityExtractor;
import ner.annotation.extraction.StanfordGazetteerPatternBuilder;

/**
 * NERAnnotator provides the framework to tag an input field with named entities.
 * Also generates all tokens for the field.
 * @author aaulabaugh@gmail.com
 */

public class NERAnnotator
{
	
	//The catalog of entity types
	protected EntityCatalog catalog;
		
	//The class responsible for determining how to interpret the annotations
	protected AnnotationReconciler reconciler;
	
	//The text to annotate
	protected String input;
	
	//All of the techniques to be used to extract entities, in order.
	protected ArrayList<EntityExtractor> extractionTechniques = new ArrayList<EntityExtractor>();
	
	//Class for JSON serialization
	protected Gson gson;
	
	//The supported serialization modes. encodedString uses Base64, JSON uses GSON
	protected final String[] SERIALIZATION_MODES = {"encodedString", "JSON"};
	protected int serialMode;
	
	//How many annotations to record on either side of an occurrence
	protected int annotationWindow;
			
	public NERAnnotator(EntityCatalog cat, String serializationSpecification, int windowSpecification)
	{
		catalog = cat;
		setSerializationMode(serializationSpecification);
		annotationWindow = windowSpecification;
	}
	
	private void setSerializationMode(String mode)
	{
		serialMode = -1;
		for(int i = 0; i < SERIALIZATION_MODES.length; i++)
		{
			if(mode.equals(SERIALIZATION_MODES[i]))
			{
				serialMode = i;
			}
		}
		switch(serialMode)
		{
			case -1:
				System.out.println("INVALID SERIALIZATION");
				break;
			case 0:
				break;
			case 1:
				gson = new GsonBuilder().create();
				break;
		}
	}
	
	/**
	 * @return the EntityCatalog associated with this annotator. This catalog
	 * contains all the supported entity types, as well as the annotations.
	 */
	public EntityCatalog getCatalog()
	{
		return catalog;
	}
	
	/**
	 * Sets the input of what's to be tagged and tokenized
	 * @param in the input field
	 */
	public void setInput(String in)
	{
		catalog.resetAnnotations();
		input = in;
	}
	
	/**
	 * Sets the input of what's to be tagged and tokenized
	 * @param in a reader of the input field
	 */
	public void setInput(Reader in)
	{
		//http://www.baeldung.com/java-convert-reader-to-string
		char[] arr = new char[8 * 1024];
		int numCharsRead;
		StringBuilder buffer = new StringBuilder();
		try
		{
		    while ((numCharsRead = in.read(arr, 0, arr.length)) != -1)
		    {
		        buffer.append(arr, 0, numCharsRead);
		    }
		    input =  buffer.toString();
		}
		catch(Exception e)
		{
			e.printStackTrace();
			input = null;
		}
	}
	
	/**
	 * Adds a new NER extraction technique to be used during annotation
	 * @param e
	 */
	public void addExtractionTechnique(EntityExtractor e)
	{
		extractionTechniques.add(e);
	}
	
	/**
	 * Sets what reconciling rules to use after annotation has been performed
	 * @param r
	 */
	public void setAnnotationReconciler(AnnotationReconciler r)
	{
		reconciler = r;
	}
	
	/**
	 * Performs NER techniques, generates annotationArray
	 */
	public void generateNERAnnotaiton()
	{		
		for(EntityExtractor extractor : extractionTechniques)
		{
			extractor.extractEntities(input);
		}
	}
	/**
	 * Performs serialization of an array list of EntityAnnotations according to the serialization mode
	 * @param list
	 * @return
	 * @throws IOException
	 */
	private String getSerializedAnnotationArray(List<EntityAnnotation> list) throws IOException
	{
		switch(serialMode)
		{
			case 0:
				ByteArrayOutputStream bos = new ByteArrayOutputStream();
				ObjectOutputStream outStream = new ObjectOutputStream(bos);
				outStream.writeObject(list);
				byte[] output = bos.toByteArray();
				outStream.close();
				bos.close();
				return Base64.encode(output);
			case 1:
				String json = gson.toJson(list);
				return json;
			default:
				return null;
		}
	}
		
	/**
	 * Performs annotation, then writes all of the EntityAnnotation to a string
	 * @return a string containing the serialized annotationArray
	 * @Deprecated use getInstanceBasedAnnotations instead for by-occurrence annotation
	 */	
	public String getSerializedAnnotation() throws IOException
	{
		if(catalog.getAnnotaitons().size() == 0)
		{
			generateNERAnnotaiton();
			catalog.reconcileAnnotation(reconciler);
		}
		
		return getSerializedAnnotationArray(catalog.getAnnotaitons());
	}
	
	/**
	 * Annotates a document, then returns serialized annotations for each Lucene field split up by entityType.
	 * @return outputAnnotations field->(ArrayList<entityAnnotation> as a string)
	 * @throws IOException
	 */
	public HashMap<String, String> getFieldBasedAnnotation() throws IOException
	{
		HashMap<String, ArrayList<EntityAnnotation>> fieldAnnotations = new HashMap<String, ArrayList<EntityAnnotation>>();
		if(catalog.getAnnotaitons().size() == 0)
		{
			generateNERAnnotaiton();
			catalog.reconcileAnnotation(reconciler);
		}
		ArrayList<EntityAnnotation> annotations = catalog.getAnnotaitons();
		StringBuilder windowText = new StringBuilder();
		for(EntityAnnotation annotation : annotations)
		{
			ArrayList<EntityType> annotationTypes = annotation.getTypes();
			annotation.clearTypes();
			
			windowText.append(annotation.getContent());
			windowText.append(" ");
			
			for(EntityType type : annotationTypes)
			{
				String typeID = type.getID();
				if(!fieldAnnotations.containsKey(typeID))
					fieldAnnotations.put(typeID, new ArrayList<EntityAnnotation>());
				fieldAnnotations.get(typeID).add(annotation);
			}
			/*
			if(annotationTypes.size() == 0)
			{
				String typeID = "keyword";
				if(!fieldAnnotations.containsKey(typeID))
					fieldAnnotations.put(typeID, new ArrayList<EntityAnnotation>());
				fieldAnnotations.get(typeID).add(annotation);
			}
			*/
			String typeID = "keyword";
			if(!fieldAnnotations.containsKey(typeID))
				fieldAnnotations.put(typeID, new ArrayList<EntityAnnotation>());
			fieldAnnotations.get(typeID).add(annotation);
		}
		windowText.deleteCharAt(windowText.length()-1);
		HashMap<String, String> outputAnnotations = new HashMap<String, String>();
		for(String fieldname : fieldAnnotations.keySet())
		{
			outputAnnotations.put(fieldname, getSerializedAnnotationArray(fieldAnnotations.get(fieldname)));
		}
		outputAnnotations.put("text", windowText.toString());
		return outputAnnotations;
	}
	
	/**
	 * Writes all EntityAnnotations to a string, but creates as many strings as there are
	 * Entity occurrences
	 * @param docNum the docID number
	 * @return an arraylist of an annotation for each entity occurance in a doc
	 * 				each of which is of the form typeName<annotationNum, typeNum, docNum>serializedAnnotations
	 */
	public ArrayList<HashMap<String, String>> getInstanceBasedAnnotations(int docNum) throws IOException
	{
		ArrayList<HashMap<String, String>> annotationOutput = new ArrayList<HashMap<String, String>>();
		if(catalog.getAnnotaitons().size() == 0)
		{
			generateNERAnnotaiton();
			catalog.reconcileAnnotation(reconciler);
		}
		ArrayList<EntityAnnotation> annotations = catalog.getAnnotaitons();
		int numAnnotations = annotations.size();
		int annotationNum = 0;
		for(EntityAnnotation annotation : annotations)
		{
			for(int typeNum = 0; typeNum < annotation.getTypes().size(); typeNum++)
			{
				HashMap<String, ArrayList<EntityAnnotation>> fieldAnnotations = new HashMap<String, ArrayList<EntityAnnotation>>();
				HashMap<String, String> instanceAnnotation = new HashMap<String, String>();
				String entityID = annotation.getTypes().get(typeNum).getID();
				String entityContent = annotation.getContent();
				int minIndex = Math.max(0, annotationNum - annotationWindow);
				int maxIndex = Math.min(numAnnotations, annotationNum + annotationWindow+1);
				@SuppressWarnings("unchecked")
				ArrayList<EntityAnnotation> subArray = new ArrayList<EntityAnnotation>(annotations.subList(minIndex, maxIndex));
				int charOffset = 0;
				int tokenOffset = 0;
				boolean seenTarget = false;
				StringBuilder windowText = new StringBuilder();
				for(EntityAnnotation b : subArray)
				{
					EntityAnnotation a = b.clone();
					if(b == annotation)
					{
						seenTarget = true;
						charOffset = windowText.length();
					}
					windowText.append(a.getContent());
					windowText.append(" ");
					
					ArrayList<EntityType> annotationTypes = a.getTypes();
					a.clearTypes();
					for(EntityType type : annotationTypes)
					{
						String typeID = type.getID();
						if(typeID.equals(entityID) && !seenTarget)
							tokenOffset+=1;
						if(!fieldAnnotations.containsKey(typeID))
							fieldAnnotations.put(typeID, new ArrayList<EntityAnnotation>());
						fieldAnnotations.get(typeID).add(a);
					}
					/*
					if(annotationTypes.size() == 0)
					{
						String typeID = "keyword";
						if(!fieldAnnotations.containsKey(typeID))
							fieldAnnotations.put(typeID, new ArrayList<EntityAnnotation>());
						fieldAnnotations.get(typeID).add(a);
					}
					*/
					String typeID = "keyword";
					if(!fieldAnnotations.containsKey(typeID))
						fieldAnnotations.put(typeID, new ArrayList<EntityAnnotation>());
					fieldAnnotations.get(typeID).add(a);
				}
				windowText.deleteCharAt(windowText.length()-1);
				
				instanceAnnotation.put("_type", entityID);
				instanceAnnotation.put("entityContent", entityContent);
				instanceAnnotation.put("annotationHeader", "<" + tokenOffset + "," + entityID + "," + docNum + "," + charOffset + ">");
				//instanceAnnotation.put("annotationContent", getSerializedAnnotationArray(subArray));
				for(String fieldname : fieldAnnotations.keySet())
				{
					instanceAnnotation.put(fieldname, getSerializedAnnotationArray(fieldAnnotations.get(fieldname)));
				}
				instanceAnnotation.put("text", windowText.toString());
				annotationOutput.add(instanceAnnotation);
				typeNum++;
			}
			annotationNum++;
		}
		return annotationOutput;
	}
	
	public void close()
	{
		for(EntityExtractor extractor : extractionTechniques)
		{
			extractor.close();
		}
	}
}
