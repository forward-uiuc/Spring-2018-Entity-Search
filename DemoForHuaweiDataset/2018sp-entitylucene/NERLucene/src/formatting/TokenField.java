package formatting;

import java.io.IOException;
import java.text.ParseException;
/* This class can be implemented to define new data types that can be handled by the postings. After a field is defined, it can be 
 * added to the schema file if the user wants such data types in their postings  */ 
public interface TokenField {

	public Object FieldContent = null;
	public String FieldType = null;
		
	public byte[] serialize(Object obj);
	public Object deserialize(byte[] data);
	public Object getContent();
	public boolean setTokenField(Object TokenContent) throws IOException, ParseException;
	public String toString();
	
}
