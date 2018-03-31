package ner.annotation;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.Reader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import com.google.gson.*;

import com.sun.org.apache.xerces.internal.impl.dv.util.Base64;

import ner.annotation.extraction.EntityExtractor;

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
	
	protected boolean USE_ES;
	
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
			
	public NERAnnotator(EntityCatalog cat, String serializationSpecification, int windowSpecification, boolean use_es)
	{
		catalog = cat;
		setSerializationMode(serializationSpecification);
		annotationWindow = windowSpecification;
		USE_ES = use_es;
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
	 * Creates
	 * @param list
	 * @param target
	 * @return
	 */
	private HashMap<String, String> getESAnnotations(List<EntityAnnotation> list, EntityAnnotation target)
	{
		int tokenPosition = 0;
		int charPosition = 0;
		HashMap<String, String> annotationHash = new HashMap<String, String>();
		HashMap<String, StringBuilder> builderHash = new HashMap<String, StringBuilder>();
		builderHash.put("text", new StringBuilder());
		int tokenNum = 0;
		for(EntityAnnotation token : list)
		{
			//System.out.println("token: "+token);
			//System.out.println("target: "+target);
			if(token == target)
			{
				tokenPosition = tokenNum;
				charPosition = builderHash.get("text").length();
			}
			tokenNum++;
			Set<String> unupdatedTypes = new TreeSet<String>(builderHash.keySet());
			for(EntityType type : token.getTypes())
			{
				String hash = Integer.toString(token.hashCode()) + Integer.toString(type.hashCode());
				String tid = type.getID();
				//System.out.println("tid: "+ tid);
				tid = tid.replaceAll(" ", "_");
				if(!builderHash.containsKey(tid))
				{
					builderHash.put(tid + "_begin", new StringBuilder(builderHash.get("text")));
					builderHash.put(tid + "_end", new StringBuilder(builderHash.get("text")));
				}
				else
				{
					unupdatedTypes.remove(tid + "_begin");
					unupdatedTypes.remove(tid + "_end");
				}
				String[] subtokens = token.getContent().split(" ");
//				for(String st:subtokens)
//					System.out.println("subtoken: "+st);
				if(subtokens.length > 1)
				{
					builderHash.get(tid + "_begin").append("oentityo|"+ hash + " " + String.join(" ", Arrays.copyOfRange(subtokens, 1, subtokens.length)) + " ");
					builderHash.get(tid + "_end").append(String.join(" ", Arrays.copyOfRange(subtokens, 0, subtokens.length-1)) + " " + "oentityo|"+ hash + " ");
				}
				else
				{
					builderHash.get(tid + "_begin").append("oentityo|"+ hash + " ");
					builderHash.get(tid + "_end").append("oentityo|"+ hash + " ");
				}
			}
			builderHash.get("text").append(token.getContent() + " ");
			unupdatedTypes.remove("text");
			for(String toUpdate : unupdatedTypes)
			{
				builderHash.get(toUpdate).append(token.getContent() + " ");
			}
		}
//		for(String k:builderHash.keySet())
//		{
//			String K = k.toString();
//			String value = builderHash.get(k).toString();
//			System.out.println("key: "+K+", "+"value: "+value);
//		}
		//https://stackoverflow.com/questions/3395286/remove-last-character-of-a-stringbuilder
		for(String builderKey : builderHash.keySet())
		{
			StringBuilder sb = builderHash.get(builderKey);
			if (sb.length() > 0)
			{
				sb.setLength(sb.length() - 1);
			}
			annotationHash.put(builderKey, sb.toString());
		}
		annotationHash.put("_posinfo", tokenPosition + "," + charPosition);
		return annotationHash;
	}
	
	@Deprecated
	private HashMap<String, String> getESAnnotationsOldStyle(List<EntityAnnotation> list, EntityAnnotation target)
	{
		int tokenPosition = 0;
		int charPosition = 0;
		HashMap<String, String> annotationHash = new HashMap<String, String>();
		HashMap<String, StringBuilder> builderHash = new HashMap<String, StringBuilder>();
		builderHash.put("text", new StringBuilder());
		int tokenNum = 0;
		for(EntityAnnotation token : list)
		{
			if(token == target)
			{
				tokenPosition = tokenNum;
				charPosition = builderHash.get("text").length();
			}
			tokenNum++;
			Set<String> unupdatedTypes = new TreeSet<String>(builderHash.keySet());
			for(EntityType type : token.getTypes())
			{
				String tid = type.getID();
				tid = tid.replaceAll(" ", "_");
				if(!builderHash.containsKey(tid))
					builderHash.put(tid, new StringBuilder(builderHash.get("text")));
				else
					unupdatedTypes.remove(tid);
				//builderHash.get(tid).append("#_[ " + token.getContent() + " #_] ");
				String[] subtokens = token.getContent().split(" ");
				if(subtokens.length > 1)
				{
					builderHash.get(tid).append("oentityo " + String.join(" ", Arrays.copyOfRange(subtokens, 1, subtokens.length)) + " ");
				}
				else
					builderHash.get(tid).append("oentityo ");
			}
			builderHash.get("text").append(token.getContent() + " ");
			unupdatedTypes.remove("text");
			for(String toUpdate : unupdatedTypes)
			{
				builderHash.get(toUpdate).append(token.getContent() + " ");
			}
		}
		//https://stackoverflow.com/questions/3395286/remove-last-character-of-a-stringbuilder
		for(String builderKey : builderHash.keySet())
		{
			StringBuilder sb = builderHash.get(builderKey);
			if (sb.length() > 0)
			{
				sb.setLength(sb.length() - 1);
			}
			annotationHash.put(builderKey, sb.toString());
		}
		annotationHash.put("_posinfo", tokenPosition + "," + charPosition);
		return annotationHash;
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
		HashMap<String, String> outputAnnotations = new HashMap<String, String>();
		HashMap<String, ArrayList<EntityAnnotation>> fieldAnnotations = new HashMap<String, ArrayList<EntityAnnotation>>();
		if(catalog.getAnnotaitons().size() == 0)
		{
			generateNERAnnotaiton();
			catalog.reconcileAnnotation(reconciler);
		}
		ArrayList<EntityAnnotation> annotations = catalog.getAnnotaitons();
		if(USE_ES)
		{
			HashMap<String, String> ESAnnotations = getESAnnotations(annotations, null);
			for(String fieldname : ESAnnotations.keySet())
			{
				String ESAnnotation = ESAnnotations.get(fieldname);
				if(ESAnnotation.length() > 0)
				{
					if(!fieldname.equals("_posinfo") && !fieldname.equals("text"))
						outputAnnotations.put("_entity_" + fieldname, ESAnnotation);
					else if(!fieldname.equals("_posinfo"))
						outputAnnotations.put(fieldname, ESAnnotation);
				}
			}
		}
		else
		{
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
				if(annotationTypes.size() == 0)
				{
					String typeID = "keyword";
					if(!fieldAnnotations.containsKey(typeID))
						fieldAnnotations.put(typeID, new ArrayList<EntityAnnotation>());
					fieldAnnotations.get(typeID).add(annotation);
				}
			}
			windowText.deleteCharAt(windowText.length()-1);
			outputAnnotations.put("text", windowText.toString());
			for(String fieldname : fieldAnnotations.keySet())
			{
				outputAnnotations.put(fieldname, getSerializedAnnotationArray(fieldAnnotations.get(fieldname)));
			}
		}		
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
	//	System.out.println(catalog);
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
				ArrayList<EntityAnnotation> subArray = new ArrayList<EntityAnnotation>(annotations.subList(minIndex, maxIndex));
				int charOffset = 0;
				int tokenOffset = 0;
				boolean seenTarget = false;
				if(USE_ES)
				{
					HashMap<String, String> ESAnnotations = getESAnnotations(subArray, annotation);
					for(String fieldname : ESAnnotations.keySet())
					{
						//System.out.println("filedname: "+fieldname);
						String ESAnnotation = ESAnnotations.get(fieldname);
						if(ESAnnotation.length() > 0)
						{
							if(fieldname.equals("text"))
								instanceAnnotation.put("text", ESAnnotation);
							else if(!fieldname.equals("_posinfo"))
								instanceAnnotation.put("_entity_" + fieldname, ESAnnotation);
							else
							{
								String[] items = ESAnnotation.split(",");
								tokenOffset = Integer.parseInt(items[0]);
								charOffset = Integer.parseInt(items[1]);
							}
						}
					}
					instanceAnnotation.put("tokenOffset", Integer.toString(tokenOffset));
					instanceAnnotation.put("charOffset", Integer.toString(charOffset));
				}
				else
				{
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
						if(annotationTypes.size() == 0)
						{
							String typeID = "keyword";
							if(!fieldAnnotations.containsKey(typeID))
								fieldAnnotations.put(typeID, new ArrayList<EntityAnnotation>());
							fieldAnnotations.get(typeID).add(a);
						}
					}
					windowText.deleteCharAt(windowText.length()-1);
					for(String fieldname : fieldAnnotations.keySet())
					{
						instanceAnnotation.put(fieldname, getSerializedAnnotationArray(fieldAnnotations.get(fieldname)));
					}
					instanceAnnotation.put("text", windowText.toString());
					instanceAnnotation.put("annotationHeader", "<" + tokenOffset + "," + entityID + "," + docNum + "," + charOffset + ">");
				}
				
				instanceAnnotation.put("entityCategory", entityID);
				instanceAnnotation.put("entityContent", entityContent);
				
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
