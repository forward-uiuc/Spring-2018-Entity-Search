package formatting;

import java.io.IOException;
import java.io.Serializable;

/* Tokencontent field that saves a float field. Internally saved as java float class */
public class FloatField implements TokenField, Serializable{
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	
	public Object FieldContent = null;
	public String FieldType = "FLOAT";
	
	
	public byte[] serialize(Object obj){
		return null;
		
	};
	public Object deserialize(byte[] data){
		return data;
		
	};
	
	public Object getContent(){
		return FieldContent;
		
	};
	
	/* Check that input is correctly formatted, and parse as java float class*/
	public boolean setTokenField(Object TokenContent) throws IOException{
		if(!TokenContent.getClass().toString().equals("class java.lang.Float")){
			throw new IOException("Input is not an float.");
		}
		FieldContent = TokenContent;
		return true;
		
	};
	
	public String toString(){
		return FieldContent.toString(); 
	};
}
