package pattern;

import java.util.ArrayList;

public class OrToken extends Token {
	public ArrayList<Token> orList;

	public OrToken(ArrayList<Token> orList) {
			this.orList = orList;
	}
}
