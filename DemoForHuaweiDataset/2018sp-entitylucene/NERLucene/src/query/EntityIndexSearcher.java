package query;

import java.io.IOException;
import java.util.Collection;

import org.apache.lucene.index.IndexReader;
import org.apache.lucene.search.CollectorManager;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.search.TopScoreDocCollector;
import org.apache.lucene.search.similarities.BM25Similarity;
import org.apache.lucene.search.similarities.Similarity;

public class EntityIndexSearcher extends IndexSearcher{
	
	public EntityIndexSearcher(IndexReader r) {
		super(r);
		setSimilarity(new EntitySimilarity());
	}
	
	/**
	 * Important notes here. The numHits is not the number of results you want to receive like document search.
	 * It's the number of top entities that will be kept in the priority queue. The actual number of results returned
	 * may be less because of the aggregation.
	 * 
	 * @author jjiang1110@gmail.com
	 */
	@Override
	public TopDocs searchAfter(ScoreDoc after, Query query, int numHits) throws IOException {
	    final int limit = Math.max(1, getIndexReader().maxDoc());
	    if (after != null && after.doc >= limit) {
	      throw new IllegalArgumentException("after.doc exceeds the number of documents in the reader: after.doc="
	          + after.doc + " limit=" + limit);
	    }

	    //final int cappedNumHits = Math.min(numHits, limit);
	    final int cappedNumHits = numHits;

	    final CollectorManager<TopEntityCollector, TopDocs> manager = new CollectorManager<TopEntityCollector, TopDocs>() {

	      @Override
	      public TopEntityCollector newCollector() throws IOException {
	        return TopEntityCollector.create(cappedNumHits);
	      }

	      @Override
	      public TopDocs reduce(Collection<TopEntityCollector> collectors) throws IOException {
	        final TopDocs[] topDocs = new TopDocs[collectors.size()];
	        int i = 0;
	        for (TopEntityCollector collector : collectors) {
	          topDocs[i++] = collector.topDocs();
	        }
	        return TopDocs.merge(0, cappedNumHits, topDocs, true);
	      }

	    };

	    return search(query, manager);
	  }
}
