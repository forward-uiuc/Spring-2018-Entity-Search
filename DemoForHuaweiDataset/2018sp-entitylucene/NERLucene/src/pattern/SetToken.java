package pattern;

import java.util.ArrayList;

public class SetToken extends Token {
	private ArrayList<Token> tokenList;
	private int windowSize;
		
	public SetToken(ArrayList<Token> tokenList, int windowSize){
		this.tokenList=tokenList;
		this.windowSize=windowSize;
	}
	
	public ArrayList<Token> getTokenList(){
		return tokenList;
	}
		
	public Integer getMaxSpan() {
		return windowSize;
	}
	

	public String entity;
	public ArrayList<String> orkeywords;
	
	public void translate(){
		orkeywords = new ArrayList();
		for(int i = 0; i < tokenList.size(); i++){
			if(tokenList.get(i) instanceof OrToken){
				OrToken orToken = (OrToken)tokenList.get(i);
				for(Token token : orToken.orList){
					KeywordToken kt = (KeywordToken)token;
					orkeywords.add(kt.term);
				}
			}
			if(tokenList.get(i) instanceof EntityToken){
				EntityToken entityToken = (EntityToken)tokenList.get(i);
				entity = entityToken.type;
			}
			if(tokenList.get(i) instanceof KeywordToken){
				KeywordToken keywordToken = (KeywordToken)tokenList.get(i);
				orkeywords.add(keywordToken.term);
			}
		}
	}
	
	
}



