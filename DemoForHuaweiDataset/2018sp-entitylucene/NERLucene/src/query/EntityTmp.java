package query;

import java.util.LinkedHashMap;

import formatting.TokenField;

public class EntityTmp {
	
	public LinkedHashMap<String, TokenField> lhm;
	public int pos;
	
	public EntityTmp(LinkedHashMap<String, TokenField> lhm, int pos){
		this.lhm = lhm;
		this.pos = pos;
	}
	
}
