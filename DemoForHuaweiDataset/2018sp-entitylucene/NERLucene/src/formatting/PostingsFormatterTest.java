package formatting;
import java.io.IOException;
import java.text.ParseException;
import java.util.LinkedHashMap;

import formatting.EntityTokenFormatter;

public class PostingsFormatterTest {
	public static void main(String[] args) throws IOException, ClassNotFoundException, ParseException{
		
		EntityTokenFormatter postings = new EntityTokenFormatter("JSON");
		postings.setTokenContent("entity","University");
		postings.setTokenContent("instance" , "UIUC");
		postings.setTokenContent("date","1993-08-16");
		postings.setTokenContent("confidence",0.78f);
		LinkedHashMap<String, TokenField> output = postings.getTokenContent().deserialize(postings.getTokenContent().serialize());

		
	}

}
