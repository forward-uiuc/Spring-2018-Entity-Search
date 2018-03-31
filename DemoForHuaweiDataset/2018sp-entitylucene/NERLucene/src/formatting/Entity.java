package formatting;

import java.util.Iterator;
import java.util.LinkedHashMap;

public class Entity {
	LinkedHashMap<String, TokenField> Fields; 
	
	public Entity(LinkedHashMap<String, TokenField> inputFields){
		Fields = inputFields;	
	}
	
	public TokenField getContent(String Field){
		return Fields.get(Field);
	}
	
	public Iterator<TokenField> iterator(){
		Iterator<TokenField> it = Fields.values().iterator();
		return it; 
	}
}
