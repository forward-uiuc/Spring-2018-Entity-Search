/*

 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */


package ner.analysis;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.Reader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.concurrent.locks.ReentrantLock;

import org.apache.lucene.analysis.Tokenizer;
import org.apache.lucene.analysis.standard.StandardTokenizer;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.apache.lucene.analysis.tokenattributes.OffsetAttribute;
import org.apache.lucene.analysis.tokenattributes.PayloadAttribute;
import org.apache.lucene.analysis.tokenattributes.PositionIncrementAttribute;
import org.apache.lucene.analysis.tokenattributes.TypeAttribute;
import org.apache.lucene.util.Attribute;
import org.apache.lucene.util.AttributeFactory;
import org.apache.lucene.util.BytesRef;

import com.sun.org.apache.xml.internal.security.exceptions.Base64DecodingException;
import com.sun.org.apache.xml.internal.security.utils.Base64;

import formatting.PostingsFormatter;
import ner.annotation.EntityAnnotation;
import ner.annotation.EntityType;

import com.google.gson.*;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.math.RoundingMode;
import java.text.DecimalFormat;

/**
 * Turns the entity-annotated output of NERAnnotator into tokens
 * and passes them to the PostingsFormatterInterface so they can be written to
 * the index
 * @author aaulabaugh@gmail.com
 */

public abstract class NERTokenizer extends Tokenizer
{
	//The number of tokens processed, -1 when the annotationArray is not set up
	protected int tokenNum = -1;	
	
	//The attributes which the PostingsFormatterInterface loads with token information
	protected final CharTermAttribute termAtt = addAttribute(CharTermAttribute.class);
	protected final OffsetAttribute offsetAtt = addAttribute(OffsetAttribute.class);
	protected final PositionIncrementAttribute posIncrAtt = addAttribute(PositionIncrementAttribute.class);
	protected final TypeAttribute typeAtt = addAttribute(TypeAttribute.class);
	protected final PayloadAttribute payAtt = addAttribute(PayloadAttribute.class);
	
	//The formatter for writing the token information to the index
	protected PostingsFormatter postingsFormatter;
	
	//The token information, parsed from the NERAnnotator
	protected ArrayList<EntityAnnotation> annotationArray;
	
	//Information on the entity instance for this doc
	protected int instanceAnnotationNum;
	protected String instanceTypeID;
	protected int docNum;
	
	//The types of ArrayList<EntityAnnotation> encodings supported
	protected final String[] INPUT_MODES = {"encodedString", "JSON"};
	protected int inputMode;
		
	private Gson gson;
	
	protected void setInputMode(String specification) throws Exception
	{
		inputMode = -1;
		for(int i = 0; i < INPUT_MODES.length; i++)
		{
			if(specification.equals(INPUT_MODES[i]))
			{
				inputMode = i;
			}
		}
		if(inputMode == -1)
		{
			throw new Exception("INVALID TOKENIZATION INPUT MODE");
		}
	}
	
	/**
	 * Converts the annotated field input from a Reader to a String
	 * @return the input as a string
	 */
	protected String readInputToString()
	{
		//http://www.baeldung.com/java-convert-reader-to-string
		char[] arr = new char[8 * 1024];
		int numCharsRead;
		StringBuilder buffer = new StringBuilder();
		try
		{
		    while ((numCharsRead = input.read(arr, 0, arr.length)) != -1)
		    {
		        buffer.append(arr, 0, numCharsRead);
		    }
		    return buffer.toString();
		}
		catch(Exception e)
		{
			e.printStackTrace();
			return "";
		}
	}

	/**
	 * Method to set up the entityArray, should be overwritten to also
	 * set up the PostingsFormatterInterface
	 */
	protected void init(String inputSpecification)
	{
		try
		{
			setInputMode(inputSpecification);
		}
		catch(Exception e)
		{
			e.printStackTrace();
		}
		gson = new GsonBuilder().create();
		annotationArray = new ArrayList<EntityAnnotation>();
		instanceAnnotationNum = -1;
		instanceTypeID = "";
	}
	
	/**
	 * Parse annotation number and type number for doc's instance from formatted string
	 * @param instancePrefix
	 */
	protected void extractInstanceInformation(String instancePrefix)
	{
		if(instancePrefix.length() > 0)
		{
			String[] prefixValues = instancePrefix.substring(1, instancePrefix.length()-1).split(",");
			instanceAnnotationNum = Integer.parseInt(prefixValues[0]);
			instanceTypeID = prefixValues[1];
			docNum = Integer.parseInt(prefixValues[2]);
		}
		else
		{
			instanceAnnotationNum = -1;
			instanceTypeID = "";
			docNum = -1;
		}
	}
		
	/**
	 * Parses EntityAnnotation objects from the input encoded string
	 * @throws IOException 
	 * @throws ClassNotFoundException 
	 * @throws Base64DecodingException 
	 */
	@SuppressWarnings("unchecked")
	protected void parseInputEncodedString() throws IOException, ClassNotFoundException, Base64DecodingException
	{
		String in = readInputToString();
		String instancePrefix = in.substring(0, in.indexOf(">")+1);
		String annotationSuffex = in.substring(in.indexOf(">")+1);
		byte[] annotation = Base64.decode(annotationSuffex);
		ByteArrayInputStream annotationStream = new ByteArrayInputStream(annotation);
		ObjectInputStream objectInput = new ObjectInputStream(annotationStream);
		annotationArray =  (ArrayList<EntityAnnotation>) objectInput.readObject();
		extractInstanceInformation(instancePrefix);
		annotationStream.close();
		objectInput.close();
	}
	
	/**
	 * Parses EntityAnnotation objects from the input JSON
	 */
	protected void parseInputJSONString()
	{
		String in = readInputToString();
		String instancePrefix = in.substring(0, in.indexOf(">")+1);
		String annotationSuffex = in.substring(in.indexOf(">")+1);
		Type arraylistAnnotation = new TypeToken<ArrayList<EntityAnnotation>>(){}.getType();
		annotationArray = gson.fromJson(annotationSuffex, arraylistAnnotation);
		extractInstanceInformation(instancePrefix);
	}
	
	/**
	 * Sends the token information to the PostingsFormatterInterface
	 * @param currentTok the current token
	 * @param type the type of entity the document represents
	 * @param docNum the physical docnum of this virtual document
	 * @throws Exception
	 */
	protected abstract void loadIntoPostingsFormatter(EntityAnnotation currentTok, int docNum) throws Exception;
	
	/**
	 * Returns a NERTokenizer initialized with the same input, serialization, and other specifications
	 * as this NERTokenizer. Does not copy the annotation array.
	 */
	protected abstract NERTokenizer cloneSelf();
	
	//Called repeatedly by the IndexWriter to get all tokens
	@Override
	public boolean incrementToken() throws IOException
	{
		if(tokenNum == -1)
		{
			try
			{
				switch(inputMode)
				{
					case 0:
						parseInputEncodedString();
						break;
					case 1:
						parseInputJSONString();
						break;
				}

			}
			catch (Exception e)
			{
				e.printStackTrace();
			}
			tokenNum = 0;
		}
		clearAttributes();
		
		try
		{
			EntityAnnotation currentTok = annotationArray.get(tokenNum);
			loadIntoPostingsFormatter(currentTok, docNum);
			postingsFormatter.commit(termAtt, offsetAtt, posIncrAtt, typeAtt, payAtt);		
			if(currentTok.isFullyIndexed())
			{
				tokenNum++;
			}
			return true;
		}
		catch(Exception e)
		{
			return false;
		}		
	}

	@Override
	public final void end() throws IOException
	{
		super.end();
		// set final offset
		offsetAtt.setOffset(0, 0);
	}

	@Override
	public void close() throws IOException
	{
		super.close();
	}

	@Override
	public void reset() throws IOException
	{
		super.reset();
		tokenNum = -1;
		annotationArray = new ArrayList<EntityAnnotation>();
		postingsFormatter.resetTokenCount();
	}
}
