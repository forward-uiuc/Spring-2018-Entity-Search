package pattern;

public class EntityToken extends SingleToken{
	String type;
	public EntityToken(String type){
		this.type = type.substring(1);
	}
}
