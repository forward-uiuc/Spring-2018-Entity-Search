package ner.analysis;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;

import formatting.EntityTokenFormatter;
import ner.annotation.EntityAnnotation;
import ner.annotation.EntityType;

/**
 * An implementation of the NERTokenizer that writes to an Instance-Inverted inde
 * using a DInvFormatter
 * @author aaulabaugh@gmail.com
 */

public class DocInvNERTokenizer extends NERTokenizer
{
	/**
	 * Constructor for Instance-Inverted tokenizer
	 * @param inputSpecification the type of input the tokenizer receives
	 */
	
	private String myInputSpec;
	private String mySerialSpec;
	private String[] entityPayloadFields;
	private String[] termPayloadFields;
	
	public DocInvNERTokenizer(String inputSpecification, String serializationSpecification, String[] entityPayloadFields, String[] termPayloadFields)
	{
		myInputSpec = inputSpecification;
		mySerialSpec = serializationSpecification;
		this.entityPayloadFields = entityPayloadFields;
		this.termPayloadFields = termPayloadFields;
		postingsFormatter = new EntityTokenFormatter(serializationSpecification, entityPayloadFields, termPayloadFields);
		init(inputSpecification);
	}

	@Override
	protected void loadIntoPostingsFormatter(EntityAnnotation currentTok, int docNum) throws Exception
	{
		HashMap<String, String> info = currentTok.getThisIterationInformation();
		String textContent = info.get("token");//(String) currentTok.getContent();
		String entityType = info.get("type");//(String) currentTok.getThisIterationType();
		int tokenPosition = Integer.parseInt(info.get("offset"));//currentTok.getPosition();
		int termNum = currentTok.getTermNum();
		postingsFormatter.setTokenContent("tokenEntityType", entityType);
		postingsFormatter.setTokenContent("instance" , textContent);
		postingsFormatter.setTokenContent("position", tokenPosition);
		postingsFormatter.setTokenContent("tokenNum", termNum);
	}
	
	@Override
	protected void extractInstanceInformation(String instancePrefix)
	{
		instanceAnnotationNum = -1;
		instanceTypeID = "";
		docNum = -1;
	}

	@Override
	protected NERTokenizer cloneSelf()
	{
		return new DocInvNERTokenizer(myInputSpec, mySerialSpec, entityPayloadFields, termPayloadFields);
	}
}
