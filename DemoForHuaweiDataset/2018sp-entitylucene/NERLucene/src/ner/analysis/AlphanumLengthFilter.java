package ner.analysis;
import org.apache.lucene.analysis.TokenStream;

import java.io.IOException;

import org.apache.lucene.analysis.FilteringTokenFilter;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;

//CITATION: org.apache.lucene.analysis.miscellaneous.LengthFilter

public class AlphanumLengthFilter extends FilteringTokenFilter {

	private final int min;
	private final int max;
	private final CharTermAttribute termAtt = addAttribute(CharTermAttribute.class);
	
	public AlphanumLengthFilter(TokenStream in, int min, int max)
	{
		super(in);
		if (min < 0) {
			throw new IllegalArgumentException("minimum length must be greater than or equal to zero");
		}
		if (min > max) {
			throw new IllegalArgumentException("maximum length must not be greater than minimum length");
		}
		this.min = min;
		this.max = max;
	}

	@Override
	protected boolean accept() throws IOException
	{
		int numAlphanum = 0;
		for(int i = 0; i < termAtt.length(); i++)
		{
			if(Character.isLetterOrDigit(termAtt.charAt(i)))
				numAlphanum++;
		}
		return numAlphanum <= max && numAlphanum >= min;
	}

}
