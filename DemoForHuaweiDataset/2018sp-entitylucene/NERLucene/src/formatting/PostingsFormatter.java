package formatting;
/* The postings formatter can be implemented for formatters that format input from analyzers in to 
 * postings. The formatter should read a schema file to define structure of the token content class. 
 * The formatter then initializes a tokencontent for the analyzer to input postings in its increment token function. 
 * All of this information is then serialized correctly into the postings. The formatter also sets all the necessary 
 * attributes of lucene for the analyzer so that the user(analyzer) has to do minimal coding.*/

import java.io.IOException;
import java.text.ParseException;
import java.util.LinkedList;

import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.apache.lucene.analysis.tokenattributes.OffsetAttribute;
import org.apache.lucene.analysis.tokenattributes.PayloadAttribute;
import org.apache.lucene.analysis.tokenattributes.PositionIncrementAttribute;
import org.apache.lucene.analysis.tokenattributes.TypeAttribute;
import org.apache.lucene.index.PostingsEnum;

public abstract class PostingsFormatter {
	
	protected TokenContent TokenContentTemplate;
	protected int lastTokenNum;
	
	public TokenContent getTokenContent(){
		return TokenContentTemplate;	
	}
	
	public void setTokenContent(String Key,Object Content) throws IOException, ParseException{
		TokenContentTemplate.getField(Key).setTokenField(Content);
	}
	
	public void resetTokenCount(){
		lastTokenNum = -1;
	}
	
	// serialize tokencontent so that it can be inserted to postings 
	protected byte[] serializeTokenContent() throws IOException{   
		return getTokenContent().serialize();
	}
	
	// serialize a subset of tokencontent so that it can be inserted to postings
	protected byte[] serializeTokenContentSubset(String[] fields) throws IOException{
		return getTokenContent().serialize(fields);
	}
	public abstract LinkedList<Entity> getPostingsList(PostingsEnum postingsEnum) throws IOException, ParseException, ClassNotFoundException;
	public abstract boolean commit(CharTermAttribute termAtt, OffsetAttribute offsetAtt, PositionIncrementAttribute posIncrAtt,TypeAttribute typeAtt,PayloadAttribute payAtt) throws IOException;
}