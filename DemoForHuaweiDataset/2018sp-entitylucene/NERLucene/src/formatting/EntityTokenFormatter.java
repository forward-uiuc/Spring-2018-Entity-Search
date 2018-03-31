package formatting;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.text.ParseException;
import java.util.*;  
import org.apache.lucene.util.BytesRef;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import formatting.PostingSchemaReader;
import org.apache.lucene.index.PostingsEnum;

import org.apache.lucene.analysis.standard.StandardTokenizer;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.apache.lucene.analysis.tokenattributes.OffsetAttribute;
import org.apache.lucene.analysis.tokenattributes.PayloadAttribute;
import org.apache.lucene.analysis.tokenattributes.PositionIncrementAttribute;
import org.apache.lucene.analysis.tokenattributes.TypeAttribute; 
/* An implementation of postings formatter that formats analyzer out put as a D inverted postings*/
public class EntityTokenFormatter extends PostingsFormatter {
	private ArrayList<String> fieldlist = new ArrayList<String>();		// list of names of fields from schema file
	private ArrayList<String> typelist = new ArrayList<String>();		// list of correspending types of fields from schema file 
	private String[] entityPayloadFields;
	private String[] termPayloadFields;
	
	public EntityTokenFormatter(String serializationMode){
		this.entityPayloadFields = new String[]{};
		this.termPayloadFields = new String[]{};
		
		TokenContentTemplate = new TokenContent(serializationMode);
	    
	    lastTokenNum = 0;
	    
	    //Get local path
	    String schemaPath = "";
	    try{
	    	String path = new File(".").getAbsolutePath();
	  		String[] pathArr = path.split("/");
	  		path = String.join("/", Arrays.copyOfRange(pathArr, 0, pathArr.length-2));
	  		String localPathsFile = path + "/LOCALPATHS.txt";
	  		List<String> lines = Files.readAllLines(Paths.get(localPathsFile));
	  		for(int i = 0; i < lines.size(); i++)
	  		{
	  			String[] line = lines.get(i).split(":");
	  			if(line[0].equals("FORMATTERSCHEMA"))
	  				schemaPath = line[1];
	  		}
	  	}
	  	catch(Exception e){
	  		e.printStackTrace();
	  	}
	    
		PostingSchemaReader Interface = new PostingSchemaReader(schemaPath);
		ArrayList<ArrayList<String>> PostingsFormat = Interface.parseXML();		// populate lists with read in data from schema 
		fieldlist = PostingsFormat.get(0);
		typelist = PostingsFormat.get(1);
		
		for (int temp = 0; temp < fieldlist.size(); temp++) {		// initialize token content with format specified in schema 
			if(typelist.get(temp).equals("TEXT")){
				TokenContentTemplate.setField(fieldlist.get(temp), new TextField());
			}
			if(typelist.get(temp).equals("INT")){
				TokenContentTemplate.setField(fieldlist.get(temp), new IntField());
			}
			if(typelist.get(temp).equals("FLOAT")){
				TokenContentTemplate.setField(fieldlist.get(temp), new FloatField());
			}
			if(typelist.get(temp).equals("DATE")){
				TokenContentTemplate.setField(fieldlist.get(temp), new DateField());
			}
		}
	}
	
	public EntityTokenFormatter(String serializationMode, String[] entityPayloadFields, String[] termPayloadFields){
		this(serializationMode);
		this.entityPayloadFields = entityPayloadFields;
		this.termPayloadFields = termPayloadFields;
	}
	
	// deserialize tokencontent so that it can used in scoring function
	public LinkedList<Entity> getPostingsList(PostingsEnum postingsEnum) throws IOException, ParseException, ClassNotFoundException{ 
		  LinkedList<Entity> ll = new LinkedList<Entity>();
		  for(int i = 0; i < postingsEnum.freq(); i++){
				postingsEnum.nextPosition();
				Entity posting = new Entity(getTokenContent().deserialize(postingsEnum.getPayload().bytes));
				ll.add(posting);
				//for(String it : lhm.keySet())
					//System.out.println(it + " " + lhm.get(it).getContent() + " ");
		  }
		  return ll;
	}
	

	
	// set all necessary attributes for the analyzer. Implementations of this function may change according to type of index. 
	@Override
	public boolean commit(CharTermAttribute termAtt, OffsetAttribute offsetAtt, PositionIncrementAttribute posIncrAtt,TypeAttribute typeAtt,PayloadAttribute payAtt) throws IOException{
		
		String entityClass = (String)(getTokenContent().getField("tokenEntityType").getContent());  // get entity from entity field of postings 
		String textContent = (String)(getTokenContent().getField("instance").getContent()); // get instance form entity field of postings 
		int position = (int)(getTokenContent().getField("position").getContent());
		int tokenNum = (int)(getTokenContent().getField("tokenNum").getContent());
		posIncrAtt.setPositionIncrement(tokenNum - lastTokenNum);
		lastTokenNum = tokenNum;
		byte[] inputBytes = null;
		
		if(this.termPayloadFields.length > 0)
			inputBytes = serializeTokenContentSubset(this.termPayloadFields);
		if(inputBytes != null)
			payAtt.setPayload(new BytesRef(inputBytes));

		char[] tokChars = textContent.toCharArray();
		termAtt.copyBuffer(tokChars, 0, tokChars.length);
		typeAtt.setType(StandardTokenizer.TOKEN_TYPES[0]);
		offsetAtt.setOffset(position, position+termAtt.length());

		return true;
	}
	
}
