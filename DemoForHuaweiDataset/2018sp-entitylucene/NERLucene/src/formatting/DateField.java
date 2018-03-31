package formatting;

import java.io.IOException;
import java.io.Serializable;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;


/* Tokencontent field that saves a date field. Internally saved as java date class */
public class DateField implements TokenField,Serializable { /**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	public Object FieldContent = null;
	public String FieldType = "DATE";

	@Override
	public byte[] serialize(Object obj) {
		// TODO Auto-generated method stub
		return null;
	}
	
	@Override
	public Object deserialize(byte[] data) {
		// TODO Auto-generated method stub
		return data;
	}
	
	@Override
	public Object getContent(){
		// TODO Auto-generated method stub
		return FieldContent;
	}
	
	/* Check that input is correctly formatted, and parse as java date class*/
	@Override
	public boolean setTokenField(Object TokenContent) throws IOException, ParseException{
		if(!TokenContent.getClass().toString().equals("class java.lang.String")){
			throw new IOException("Input is not a string.");
		}
		DateFormat df = new SimpleDateFormat("yyyy-MM-dd");
		Date ParsedTokenDate = df.parse((String)TokenContent);
		System.out.println(ParsedTokenDate);
			
		FieldContent = ParsedTokenDate;
		return true; 
		
	};
	
	public String toString(){
		return FieldContent.toString(); 
	};
}
	
