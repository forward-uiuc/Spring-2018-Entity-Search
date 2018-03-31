package formatting;

import java.io.IOException;
import java.io.Serializable;

/* Tokencontent field that saves a int field. Internally saved as java string class */

public class TextField implements TokenField,Serializable {
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	
	public Object FieldContent = null;
	public String FieldType = "TEXT";
	
	
	public byte[] serialize(Object obj){
		return null;
		
	};
	public Object deserialize(byte[] data){
		return data;
		
	};
	public Object getContent(){
		return FieldContent;
		
	};
	
	/* Check that input is correctly formatted, and parse as java string class*/
	public boolean setTokenField(Object TokenContent) throws IOException{
		if(!TokenContent.getClass().toString().equals("class java.lang.String")){
			throw new IOException("Input is not a String.");
		}
			
		FieldContent = TokenContent;
		return true; 
		
	};
	
	public String toString(){
		return FieldContent.toString(); 
	};
}
