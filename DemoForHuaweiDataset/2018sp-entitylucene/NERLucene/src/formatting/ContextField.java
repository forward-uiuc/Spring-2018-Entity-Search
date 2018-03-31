package formatting;

import java.io.IOException;
import java.io.Serializable;
import java.text.ParseException;
import java.util.ArrayList;

public class ContextField implements TokenField,Serializable {
	private static final long serialVersionUID = 1L;
	
	public Object FieldContent = null;
	public String FieldType = "CONTEXT";
	
	@Override
	public byte[] serialize(Object obj) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Object deserialize(byte[] data) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Object getContent() {
		// TODO Auto-generated method stub
		return FieldContent;
	}

	@Override
	public boolean setTokenField(Object TokenContent) throws IOException{
		if(!TokenContent.getClass().toString().equals("class java.util.ArrayList")){
			throw new IOException("Input is not an context array list.");
		}
		if(((ArrayList) TokenContent).size() == 0){
			FieldContent = TokenContent;
			return true; 
		}
		else{
			if(Serializable.class.isInstance(((ArrayList)TokenContent).get(0))){
			FieldContent = TokenContent;
			return true; 
			}
			else{
				throw new IOException("Input array element is not serializable.");
			}
		}

		
	};
	
	public String toString(){
		return FieldContent.toString(); 
	};
}
