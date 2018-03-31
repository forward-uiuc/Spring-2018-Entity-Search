import java.io.IOException;
import java.nio.file.Paths;

import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.search.BooleanClause.Occur;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.SimpleFSDirectory;

import query.Aggregator;
import query.EntityIndexSearcher;
import query.EntityTermQuery;
import query.PatternQueryHandler;

public class TestQuery {
	public static void main(String[] args){
		//Searching the E-InvertedIndex
		Directory indexDirectoryE;
		try {
			indexDirectoryE = new SimpleFSDirectory(Paths.get("src/indexdirE"));
			IndexReader reader = DirectoryReader.open(indexDirectoryE);
		    IndexSearcher searcher = new EntityIndexSearcher(reader);
		    PatternQueryHandler handler = new PatternQueryHandler("[(is | cool) #person]<10>", searcher, reader, "encodedString");
		    //PatternQueryHandler handler = new PatternQueryHandler("{is cool #person}", searcher, reader, "encodedString");
		    //PatternQueryHandler handler = new PatternQueryHandler("[nothing #person]<100>", searcher, reader, "encodedString");
		    handler.execute();
		 	
		 	reader.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
	    
	}
}
