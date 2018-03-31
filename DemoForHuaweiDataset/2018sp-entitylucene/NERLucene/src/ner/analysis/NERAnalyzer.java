package ner.analysis;

import java.io.IOException;
import java.io.Reader;


import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.LowerCaseFilter;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.Analyzer.TokenStreamComponents;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.analysis.standard.StandardFilter;
import org.apache.lucene.analysis.standard.StandardTokenizer;

/**
 * Analyzer for parsing NERAnnotator's annotated output
 * @author aaulabaugh@gmail.com
 */

public class NERAnalyzer extends Analyzer {

	
	private NERTokenizer src;
	
	public NERAnalyzer()
	{
		super();
	}
	
	
	public void setTokenizer(NERTokenizer tokenizer)
	{
		src = tokenizer;
	}
	
	@Override
	protected TokenStreamComponents createComponents(String fieldName)
	{
		final NERTokenizer perThreadTokenizer = src.cloneSelf();
		TokenStream tok = new AlphanumLengthFilter(new StandardFilter(perThreadTokenizer), 3, 50);
		tok = new LowerCaseFilter(tok);
		return new TokenStreamComponents(perThreadTokenizer, tok)
		{
			@Override
			protected void setReader(final Reader reader)
			{
				super.setReader(reader);
			}
		};
	}
}



