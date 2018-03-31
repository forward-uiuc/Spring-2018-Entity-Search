package query;


import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedList;

import org.apache.lucene.index.LeafReaderContext;
import org.apache.lucene.search.LeafCollector;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.Scorer;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.search.TopDocsCollector;

import formatting.TokenField;
import ner.annotation.EntityAnnotation;


public abstract class TopEntityCollector extends TopDocsCollector<ScoreDoc> {

  abstract static class EntityCollector implements LeafCollector {

    Scorer scorer;

    @Override
    public void setScorer(Scorer scorer) throws IOException {
      this.scorer = scorer;
    }

  }

  private static class SimpleTopEntityCollector extends TopEntityCollector {
	  
    SimpleTopEntityCollector(int numHits) {
      super(numHits);
    }

    @Override
    public LeafCollector getLeafCollector(LeafReaderContext context)
        throws IOException {
      final int docBase = context.docBase;
      return new EntityCollector() {

    	/**
    	 *   Scores and collects the entities that matches the type.
    	 *   @author jjiang1110@gmail.com
    	 */
        @Override
        public void collect(int doc) throws IOException {
	          LinkedList<EntityTmp> entities = null;
	          try {
	        	  entities = ((EntityScorer) scorer).getEntity();  	  
	          } catch (ClassNotFoundException e) {
	        	  e.printStackTrace();
	          }
	          
	          String type = ((EntityScorer)scorer).getType();
	          int window = ((EntityScorer)scorer).getWindow();
	
	          for(EntityTmp tmp : entities){
	        	  LinkedHashMap<String, TokenField> lhm = tmp.lhm;
	        	  ArrayList<EntityAnnotation> annotationList = (ArrayList<EntityAnnotation>) lhm.get("context").getContent();
	        	  for(EntityAnnotation ea : annotationList){
	        		  if(ea.getTypes().get(0).getID().equals(type)){
	      		 		  if(window != -1 && Math.abs(tmp.pos - ea.getTermNum()) > window){
	      		 			  continue;
	      		 		  }
	        			  float score = ((EntityScorer)scorer).score(tmp.pos, ea.getTermNum());
				          assert score != Float.NEGATIVE_INFINITY;
				          assert !Float.isNaN(score);
				
				          totalHits++;
				          if (score <= pqTop.score) {
				            continue;
				          }
				          pqTop.doc = doc + docBase;
				          pqTop.score = score;
				          pqTop.entity = ea;
				          pqTop = (EntityScoreDoc)pq.updateTop();
	        		  }
	        	  }      	  
	          }
          }
      };
    }

  }


  public static TopEntityCollector create(int numHits) {
    return create(numHits, null);
  }

  public static TopEntityCollector create(int numHits, EntityScoreDoc after) {

    if (numHits <= 0) {
      throw new IllegalArgumentException("numHits must be > 0; please use TotalHitCountCollector if you just need the total hit count");
    }
    return new SimpleTopEntityCollector(numHits);
  }

  EntityScoreDoc pqTop;

  // prevents instantiation
  TopEntityCollector(int numHits) {
    super(new EntityHitQueue(numHits, true));
    pqTop = (EntityScoreDoc)pq.top();
  }

  @Override
  protected TopDocs newTopDocs(ScoreDoc[] results, int start) {
    if (results == null) {
      return EMPTY_TOPDOCS;
    }
    float maxScore = Float.NaN;
    if (start == 0) {
      maxScore = results[0].score;
    } else {
      for (int i = pq.size(); i > 1; i--) { pq.pop(); }
      maxScore = pq.pop().score;
    }

    return new TopDocs(totalHits, results, maxScore);
  }

  @Override
  public boolean needsScores() {
    return true;
  }
}
