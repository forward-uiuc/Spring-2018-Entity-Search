package indexing;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Scanner;

import org.apache.lucene.codecs.simpletext.SimpleTextCodec;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.FieldType;
import org.apache.lucene.document.LongPoint;
import org.apache.lucene.document.StoredField;
import org.apache.lucene.document.StringField;
import org.apache.lucene.index.IndexOptions;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.index.IndexableField;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.SimpleFSDirectory;

import com.google.gson.reflect.TypeToken;

import ner.analysis.DocInvNERTokenizer;
import ner.analysis.InstanceInvNERTokenizer;
import ner.analysis.NERAnalyzer;
import ner.annotation.EntityCatalog;
import ner.annotation.NERAnnotator;

public class WholeDocumentIndexingManager extends IndexingManager
{
	public WholeDocumentIndexingManager(String inFormat, String outFormat, Boolean simpleText, String[] entityPayloadFields, String[] termPayloadFields)
	{
		super(inFormat, outFormat, simpleText, -1, false, entityPayloadFields, termPayloadFields);
	}
	
	
	@Override
	public void annotateDocsFromTextfileDirectory(int start, int end, NERAnnotator annotator, String destination) throws IOException
	{
		String destinationPath = "src/indexdir/" + destination;
		File destinationDirectory = new File(destinationPath);
		//https://stackoverflow.com/questions/3775694/deleting-folder-from-java
		if(destinationDirectory.exists())
		{
			File[] subfiles = destinationDirectory.listFiles();
			if(subfiles != null)
			{
				for(File subfile : subfiles)
					subfile.delete();
			}
			destinationDirectory.delete();
		}
		destinationDirectory.mkdir();
		// CITATION:
		// https://www.tutorialspoint.com/lucene/lucene_indexing_process.htm
		for (int docnum = start; docnum < end; docnum++)
		{
			File file = filesToIndex[docnum];
			// if the file is valid
			if (file.isDirectory() || file.isHidden() || !file.exists() || !file.canRead() || !filter.accept(file))
			{
				continue;
			}
			Document luceneDoc = getDocFromFile(file, annotator, docnum);
			if (luceneDoc != null)
			{
				File fileToWrite = new File(destinationPath + "/" + "ldoc" + docnum + ".txt");
				fileToWrite.createNewFile();
				HashMap<String, String> docAsHash = new HashMap<String, String>();
				for(IndexableField field : luceneDoc.getFields())
				{
					docAsHash.put(field.name(), field.stringValue());
				}
				String json = gson.toJson(docAsHash);
				FileOutputStream fileOut = new FileOutputStream(fileToWrite);
				fileOut.write(json.getBytes());
				fileOut.close();
			}
		}
		annotator.close();
	}
	
	/**
	 * for(String entityType : fieldAnnotations.keySet())
			{
				if(entityType == "text")
				{
					Field textField = new StoredField("text", fieldAnnotations.get(entityType));
					doc.add(textField);
				}
				else
				{
					Field eField = new Field(entityType, fieldAnnotations.get(entityType), contentType);
					doc.add(eField);
				}
			}
			
			//Field contentField = new StoredField("annotation", annotation);
			Field nameField = new StringField("name", f.getName(), Field.Store.YES);
			BasicFileAttributes attr = Files.readAttributes(f.toPath(), BasicFileAttributes.class);			
			Field sizeField = new LongPoint("size", attr.size());
	 */
	
	@SuppressWarnings("unchecked")
	public void addDocsFromLucenedocDirectory(int start, int end) throws IOException
	{
		for (int docnum = start; docnum < end; docnum++)
		{
			File file = filesToIndex[docnum];
			// if the file is valid
			if (file.isDirectory() || file.isHidden() || !file.exists() || !file.canRead() || !filter.accept(file))
			{
				continue;
			}
			Scanner fileScan = new Scanner(file);
			StringBuilder docText = new StringBuilder();
			while(fileScan.hasNextLine())
				docText.append(fileScan.nextLine());
			Type type = new TypeToken<HashMap<String, String>>(){}.getType();
			HashMap<String, String >docAsHash = gson.fromJson(docText.toString(), type);
			Document luceneDoc = new Document();
			
			FieldType contentType = new FieldType();
			contentType.setStored(false);
			contentType.setTokenized(true);
			contentType.setOmitNorms(false);
			contentType.setIndexOptions(IndexOptions.DOCS_AND_FREQS_AND_POSITIONS_AND_OFFSETS);
			contentType.setStoreTermVectors(false);
						
			for(String fieldName : docAsHash.keySet())
			{
				Field thisField = null;
				switch (fieldName)
				{
					case "text":
						thisField = new StoredField(fieldName, docAsHash.get(fieldName));
						break;
					case "name":
						thisField = new StringField(fieldName, docAsHash.get(fieldName), Field.Store.YES);
						break;
					case "size":
						thisField = new LongPoint(fieldName, Integer.parseInt(docAsHash.get(fieldName)));
						break;
					default:
						thisField = new Field(fieldName, docAsHash.get(fieldName), contentType);
						break;
					
				}
				if(thisField != null)
					luceneDoc.add(thisField);
			}
			if (luceneDoc != null)
			{
				indexWriters.get("dinv").addDocument(luceneDoc);
			}
			fileScan.close();
		}
	}
	
	@Override
	public void addDocsFromTextfileDirectory(int start, int end, NERAnnotator annotator) throws IOException
	{
		for (int docnum = start; docnum < end; docnum++)
		{
			File file = filesToIndex[docnum];
			// if the file is valid
			if (file.isDirectory() || file.isHidden() || !file.exists() || !file.canRead() || !filter.accept(file))
			{
				continue;
			}
			Document doc = getDocFromFile(file, annotator, docnum);
			indexWriters.get("dinv").addDocument(doc);
		}
	}
	
	/**
	 * Gets a Lucene Document from a textfile on disk
	 * @param f the textfile
	 * @param annotator
	 * @param docNum
	 * @return
	 */
	private Document getDocFromFile(File f, NERAnnotator annotator, int docNum)
	{
		Document doc;
		try
		{
			doc = new Document();
			FieldType contentType = new FieldType();
			contentType.setStored(false);
			contentType.setTokenized(true);
			contentType.setOmitNorms(false);
			contentType.setIndexOptions(IndexOptions.DOCS_AND_FREQS_AND_POSITIONS_AND_OFFSETS);
			contentType.setStoreTermVectors(false);
			annotator.setInput(new String(Files.readAllBytes(f.toPath()), StandardCharsets.UTF_8));
			//String annotation = annotator.getSerializedAnnotation();
			
			HashMap<String, String> fieldAnnotations = annotator.getFieldBasedAnnotation();
			
			for(String entityType : fieldAnnotations.keySet())
			{
				if(entityType == "text")
				{
					Field textField = new StoredField("text", fieldAnnotations.get(entityType));
					doc.add(textField);
				}
				else
				{
					Field eField = new Field(entityType, fieldAnnotations.get(entityType), contentType);
					doc.add(eField);
				}
			}
			
			//Field contentField = new StoredField("annotation", annotation);
			Field nameField = new StringField("name", f.getName(), Field.Store.YES);
			BasicFileAttributes attr = Files.readAttributes(f.toPath(), BasicFileAttributes.class);			
			Field sizeField = new LongPoint("size", attr.size());
			
			//doc.add(contentField);
			doc.add(nameField);
			doc.add(sizeField);
			
		}
		catch(Exception e)
		{
			e.printStackTrace();
			return null;
		}
		return doc;
	}
	
	@Override
	public void createIndexWriters(EntityCatalog catalogClone) throws IOException
	{
		Directory indexDirectory = new SimpleFSDirectory(Paths.get("src/indexdir/dinv"));
		NERAnalyzer nerAnalyzer = new NERAnalyzer();
		nerAnalyzer.setTokenizer(new DocInvNERTokenizer(inputFormat, outputFormat, entityPayloadFields, termPayloadFields));
		IndexWriterConfig nerConfig = new IndexWriterConfig(nerAnalyzer);
		//Use for readable postings
		if(useSimpleText)
			nerConfig.setCodec(new SimpleTextCodec());
		nerConfig.setOpenMode(IndexWriterConfig.OpenMode.CREATE);
		IndexWriter typeIndexWriter = new IndexWriter(indexDirectory, nerConfig);
		indexWriters.put("dinv", typeIndexWriter);
	}
	
}
