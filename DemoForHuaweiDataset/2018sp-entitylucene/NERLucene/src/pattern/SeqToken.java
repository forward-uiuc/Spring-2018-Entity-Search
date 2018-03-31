package pattern;

import java.util.ArrayList;

public class SeqToken extends Token{

	public ArrayList<Token> tokenList;
	
	public SeqToken(ArrayList<Token> tokenList){
		this.tokenList = tokenList;
	}
	
	
	public String entity;
	public ArrayList<String> andkeywords;
	
	public void translate(){
		andkeywords = new ArrayList();
		for(int i = 0; i < tokenList.size(); i++){
			if(tokenList.get(i) instanceof SeqToken){
				SeqToken seqToken = (SeqToken)tokenList.get(i);
				for(Token token : seqToken.tokenList){
					andkeywords.add(((KeywordToken)token).term);
				}
			}
			if(tokenList.get(i) instanceof EntityToken){
				EntityToken entityToken = (EntityToken)tokenList.get(i);
				entity = entityToken.type;
			}
			if(tokenList.get(i) instanceof KeywordToken){
				KeywordToken keywordToken = (KeywordToken)tokenList.get(i);
				andkeywords.add(keywordToken.term);
			}
		}
	}

	
}
