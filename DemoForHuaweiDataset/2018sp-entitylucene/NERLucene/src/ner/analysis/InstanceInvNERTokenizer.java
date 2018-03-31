package ner.analysis;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;

import formatting.EntityTokenFormatter;
import ner.annotation.EntityAnnotation;
import ner.annotation.EntityType;

/**
 * An implementation of the NERTokenizer that writes to an Instance-Inverted index
 * @author aaulabaugh@gmail.com
 */

public class InstanceInvNERTokenizer extends NERTokenizer
{
	/**
	 * Constructor for Instance-Inverted tokenizer
	 * @param inputSpecification the type of input the tokenizer receives
	 */
	
	private String myInputSpec;
	private String mySerialSpec;
	private int instanceZeroPoint;
	private String[] entityPayloadFields;
	private String[] termPayloadFields;
	
	public InstanceInvNERTokenizer(String inputSpecification, String serializationSpecification, int instPos, String[] entityPayloadFields, String[] termPayloadFields)
	{
		myInputSpec = inputSpecification;
		mySerialSpec = serializationSpecification;
		this.entityPayloadFields = entityPayloadFields;
		this.termPayloadFields = termPayloadFields;
		postingsFormatter = new EntityTokenFormatter(serializationSpecification, entityPayloadFields, termPayloadFields);
		instanceZeroPoint = instPos;
		init(inputSpecification);
	}

	@Override
	protected void loadIntoPostingsFormatter(EntityAnnotation currentTok, int docNum) throws Exception
	{
		
		HashMap<String, String> info = currentTok.getThisIterationInformation();
		String textContent = info.get("token");//(String) currentTok.getContent();
		String entityType = info.get("type");//(String) currentTok.getThisIterationType();
		int tokenPosition = Integer.parseInt(info.get("offset"));//currentTok.getPosition();
		int termNum = currentTok.getTermNum() - instanceAnnotationNum + instanceZeroPoint;
		postingsFormatter.setTokenContent("tokenEntityType", entityType);
		postingsFormatter.setTokenContent("physicalDocNum", docNum);
		postingsFormatter.setTokenContent("instance" , textContent);
		postingsFormatter.setTokenContent("position", tokenPosition);
		postingsFormatter.setTokenContent("tokenNum", termNum);
		postingsFormatter.setTokenContent("docEntityType", instanceTypeID);
	}

	@Override
	protected NERTokenizer cloneSelf()
	{
		return new InstanceInvNERTokenizer(myInputSpec, mySerialSpec, instanceZeroPoint, entityPayloadFields, termPayloadFields);
	}
}
