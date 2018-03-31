package ner.annotation.extraction;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;

import ner.annotation.EntityAnnotation;
import ner.annotation.EntityCatalog;
import ner.annotation.extraction.huawei_tagging.QueryTaggingAutoServer;

public class HuaweiDictionaryExtractor extends EntityExtractor
{
	private HashMap<Integer, String> annotations;
	private String dictFolder;
	public HuaweiDictionaryExtractor(EntityCatalog cat, String dictFolder)
	{
		super(cat);
		this.dictFolder = dictFolder;
		annotations = new HashMap<Integer, String>();
	}
	
	public void generateAnnotation(String inputPath)
	{
		String[] dictionaryPaths=new String[8];
		dictionaryPaths[0]= dictFolder + "/generalwords.v2.csv";
		dictionaryPaths[1]= dictFolder + "/professionalword_v13.txt";
		dictionaryPaths[2]= dictFolder + "/product_v13.txt";
		dictionaryPaths[3]= dictFolder + "/symptom_v13.txt";
		dictionaryPaths[4]= dictFolder + "/A_v13.txt";
		dictionaryPaths[5]= dictFolder + "/H_v13.txt";
		dictionaryPaths[6]= dictFolder + "/signalword_v13.txt";
		dictionaryPaths[7]= dictFolder + "/F_v13.txt";
		String outputPath= dictFolder + "/entitySearchLabelResults";
		
		//System.out.println(dictFolder);
		
//		String []paths=new String[8];
//		String inputpath="/Users/apple/sp18/EntitySearch/intermediate/intermediate_3.txt";
//		String outputpath="/Users/apple/sp18/EntitySearch/StandardTestQuestion_QuestionIDLabelResults.txt";
//		String tempfileName="/Users/apple/sp18/EntitySearch/StandardTestQuestion_QuestionIDLabelResults1.txt";
//		paths[0]="/Users/apple/sp18/EntitySearch/dictionaries/generalwords.v2.csv";
//		paths[1]="/Users/apple/sp18/EntitySearch/dictionaries/professionalword_v13.txt";
//		paths[2]="/Users/apple/sp18/EntitySearch/dictionaries/product_v13.txt";
//		paths[3]="/Users/apple/sp18/EntitySearch/dictionaries/symptom_v13.txt";
//		paths[4]="/Users/apple/sp18/EntitySearch/dictionaries/A_v13.txt";
//		paths[5]="/Users/apple/sp18/EntitySearch/dictionaries/H_v13.txt";
//		paths[6]="/Users/apple/sp18/EntitySearch/dictionaries/F_v13.txt";
//		paths[7]="/Users/apple/sp18/EntitySearch/dictionaries/signalword_v13.txt";
	
		QueryTaggingAutoServer tagger = new QueryTaggingAutoServer();
		tagger.runAnnotation(dictionaryPaths, outputPath, inputPath);
		
		try
		{
			BufferedReader huaweiFile = new BufferedReader(new FileReader(outputPath));
			String annotatedDoc = "";
			int lineNum = 0;
			while((annotatedDoc = huaweiFile.readLine()) != null)
			{
				annotations.put(lineNum, annotatedDoc);
				lineNum++;
			}
			huaweiFile.close();
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
	}

	@Override
	public void extractEntities(String text)
	{
		String[] components = text.split(",");
		int lineNum = Integer.parseInt(components[0]);
		String inputPath = components[1];
		if(lineNum ==0)
			generateAnnotation(inputPath);
		if(!annotations.containsKey(lineNum))
			return;
		String annotation = annotations.get(lineNum);
		String[] annotationComponents = annotation.split("\t");
		
		for(int i = 1; i < annotationComponents.length-2; i++)
		{
			String token = annotationComponents[i];
			//System.out.println("token: "+token);
			if(token.length()<3)
			{
				continue;
			}
			try
			{
				String [] tokens=token.split("\\#");
				for(String t1: tokens)
				{
					
					String[] split1 = t1.split("_");
					String[] split2 = split1[1].split("&");
					String content = split1[0];
					String category = split2[0];
					String offset = split1[2];
					EntityAnnotation newAnnotation = new EntityAnnotation();
					newAnnotation.setContent(content);
					newAnnotation.addType(catalog.getEntityType(category));
					newAnnotation.setPosition(Integer.parseInt(offset));
					newAnnotation.setSource("HUAWEIDICT");
					catalog.addAnnotation(newAnnotation);

				}
//				String[] split1 = token.split("_");
//				String[] split2 = split1[1].split("&");
//				String content = split1[0];
//				String category = split2[0];
//				String offset = split1[2];
//				EntityAnnotation newAnnotation = new EntityAnnotation();
//				newAnnotation.setContent(content);
//				newAnnotation.addType(catalog.getEntityType(category));
//				newAnnotation.setPosition(Integer.parseInt(offset));
//				newAnnotation.setSource("HUAWEIDICT");
//				catalog.addAnnotation(newAnnotation);
			}
			catch(Exception e)
			{
				System.out.println(e.getMessage());
				System.out.println("Unexpected tagging output: " + token);
			}
		}
	}

}
