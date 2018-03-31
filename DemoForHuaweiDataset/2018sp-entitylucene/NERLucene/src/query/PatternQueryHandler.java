package query;

import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.LinkedList;

import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.BooleanClause.Occur;

import pattern.PatternLexer;
import pattern.PatternParser;
import pattern.SeqToken;
import pattern.SetToken;
import pattern.Token;

import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.search.TopDocs;

/**
 * This class is the entrance of the system.
 * @author jjiang1110@gmail.com
 */

public class PatternQueryHandler {
	PatternLexer Lexer=null;
	PatternParser Parser=null;
	String Pattern = null;
	Token token = null;
	IndexSearcher indexSearcher;
	IndexReader reader;
	String mode;
	Aggregator agg;
	
	/**
	 * 
	 * @param pattern : one query example {[(UIUC | illinois) #person]/<10/>}
	 * @param indexSearcher : {entityIndexSearcher}
	 * @param mode : {encodedString, JSON}
	 * @throws Exception
	 */
	public PatternQueryHandler(String pattern, IndexSearcher indexSearcher, IndexReader reader, String mode) throws Exception{
		Pattern = pattern;
		if (Lexer==null){
			Lexer=new PatternLexer(new StringReader(pattern));
			Parser=new PatternParser(Lexer);
		}else{
			Lexer.yyreset(new StringReader(pattern));
		}	
		token = (Token) Parser.parse().value;
		this.indexSearcher = indexSearcher;
		this.reader = reader;
		this.mode = mode;
	}
	
	/**
	 * For window pattern, just collect all the entities and accumulate their scores.
	 * For conjunction pattern, first get the valid documents and then get entities and use the docId to check valid entities.
	 * @throws IOException
	 */
	public void execute() throws IOException{
		if(token instanceof SetToken){
			SetToken setToken = (SetToken)token;
			setToken.translate();
			LinkedList<TopDocs> tds = new LinkedList();
			for(int i = 0; i < setToken.orkeywords.size(); i++){
				TopDocs td = indexSearcher.search(new EntityTermQuery(new Term("content", setToken.orkeywords.get(i)), setToken.entity, mode, setToken.getMaxSpan()), 100);	
				tds.add(td);
			}
			agg = new Aggregator(tds, null);
			System.out.println(agg);
		}else{
			SeqToken seqToken = (SeqToken)token;
			seqToken.translate();
			BooleanQuery.Builder builder = new BooleanQuery.Builder();
			for(int i = 0; i < seqToken.andkeywords.size(); i++){
				builder.add(new TermQuery(new Term("content", seqToken.andkeywords.get(i))), Occur.MUST);
			}
			BooleanQuery bq = builder.build();
			IndexSearcher docSearcher = new IndexSearcher(reader);
			TopDocs docTd = docSearcher.search(bq, 100);
			LinkedList<Integer> validDocID = new LinkedList();
			for(ScoreDoc sd : docTd.scoreDocs){
				validDocID.add(sd.doc);
			}
			
			LinkedList<TopDocs> tds = new LinkedList();
			for(int i = 0; i < seqToken.andkeywords.size(); i++){
				TopDocs td = indexSearcher.search(new EntityTermQuery(new Term("content", seqToken.andkeywords.get(i)), seqToken.entity, mode, -1), 100);	
				tds.add(td);
			}
			agg = new Aggregator(tds, validDocID);
			System.out.println(agg);
		}
	}

}





