package formatting;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.lang.reflect.Type;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.SortedMap;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;

/* The token content is the internal hashmap that saves the postings field data before it is flushed into the actual lucene postings  */ 
public class TokenContent implements Comparator<TokenContent>, Comparable<TokenContent>
{
	private LinkedHashMap<String, TokenField> DefinedFields ;	//field data will be saved in this hashmap. A linked hashmap is used because it has order. They will be serialized to the postings in an ordered fashion.
	private int beginIndex;
	private int endIndex;
	
	protected final String[] SERIALIZATION_MODES = {"encodedString", "JSON"};
	protected int serialMode;
	
	protected Gson gson;
	
	public TokenContent()
	{
		DefinedFields = new LinkedHashMap<String, TokenField>();
		beginIndex = -1;
		endIndex = -1;
	}	
	
	public TokenContent(String serializationMode)
	{
		DefinedFields = new LinkedHashMap<String, TokenField>();
		beginIndex = -1;
		endIndex = -1;
		setSerializationMode(serializationMode);
	}
	
	public TokenContent(String serializationMode, int beginIndex_in, int endIndex_in)
	{
		DefinedFields = new LinkedHashMap<String, TokenField>();
		beginIndex = beginIndex_in;
		endIndex = endIndex_in;
		setSerializationMode(serializationMode);
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
	
	public void setAllFields(LinkedHashMap<String, TokenField> allFields) throws IOException, ParseException{ 
		DefinedFields = allFields;
	}
	
	public void setField(String identifier, TokenField fieldIn)
	{
		DefinedFields.put(identifier, fieldIn);
	}
	
	public TokenField getField(String identifier)
	{
		try
		{
			return DefinedFields.get(identifier);
		}
		catch(Exception e)
		{
			return null;
		}
	}
	
	public void setBeginIndex(int beginIndex_in)
	{
		beginIndex = beginIndex_in;
	}
	
	public int getBeginIndex()
	{
		return beginIndex;
	}
	
	public void setEndIndex(int endIndex_in)
	{
		endIndex = endIndex_in;
	}
	
	public int getEndIndex()
	{
		return endIndex;
	}

	@Override
	public int compareTo(TokenContent o)
	{
		return beginIndex - o.getBeginIndex();
	}

	@Override
	public int compare(TokenContent o1, TokenContent o2)
	{
		return o1.getBeginIndex() - o2.getBeginIndex();
	}
	
	/**
	 * serializes a subset of the fields in this TokenContent
	 * @param serializedFields a list of the fields to be serialized
	 * @return
	 * @throws IOException
	 */
	public byte[] serialize(String[] serializedFields) throws IOException
	{
		LinkedHashMap<String, TokenField> subsetMap = new LinkedHashMap<String, TokenField>();
		for(String fieldName : serializedFields)
		{
			subsetMap.put(fieldName, DefinedFields.get(fieldName));
		}
		switch(serialMode)
		{
			case 0: //encodedString
				ByteArrayOutputStream bos = new ByteArrayOutputStream();
				ObjectOutputStream oos = new ObjectOutputStream(bos);
				oos.writeObject(subsetMap);
				byte[] bytes = bos.toByteArray();
				return bytes;
				
			case 1: //JSON
				String json = gson.toJson(subsetMap);
				return json.getBytes();
		}
		return null;
	}
	
	public  byte[]  serialize() throws IOException			// serialize the postings. This is possible because all fields in the postings extends java Serializable. 
	{
		switch(serialMode)
		{
			case 0: //encodedString
				ByteArrayOutputStream bos = new ByteArrayOutputStream();
				ObjectOutputStream oos = new ObjectOutputStream(bos);
				oos.writeObject(DefinedFields);
				byte[] bytes = bos.toByteArray();
				return bytes;
				
			case 1: //JSON
				String json = gson.toJson(DefinedFields);
				Type arraylistAnnotation = new TypeToken<LinkedHashMap<String, TokenField>>(){}.getType();
				return json.getBytes();
		}
		return null;
	}
	

	@SuppressWarnings("unchecked")
	public LinkedHashMap<String, TokenField> deserialize(byte[] bytes) throws IOException, ClassNotFoundException   // return the linked hashmap containing all fields and their data from a byte array. 
	{
		
		switch(serialMode)
		{
			case 0: //encodedString
				ByteArrayInputStream bos = new ByteArrayInputStream(bytes);
				ObjectInputStream ois = new ObjectInputStream(bos);
				return (LinkedHashMap<String, TokenField>) ois.readObject();
				
			//NOTE: BUG HERE
			case 1: //JSON LinkedHashMap<String, TokenField>
				Type arraylistAnnotation = new TypeToken<LinkedHashMap<String, TokenField>>(){}.getType();
				return gson.fromJson(new String(bytes, "UTF-8"), arraylistAnnotation);
		}
		return null;
	}
}

