package query;


import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.util.PriorityQueue;

/**
 * Prepopulate the priority queue. Also override the function of comparing the scores of two entities.
 * @author jjiang1110@gmail.com
 */

final class EntityHitQueue extends PriorityQueue<ScoreDoc> {
	EntityHitQueue(int size, boolean prePopulate) {
    super(size, prePopulate);
  }

  @Override
  protected ScoreDoc getSentinelObject() {
    return new EntityScoreDoc(Integer.MAX_VALUE, Float.NEGATIVE_INFINITY);
  }
  
  @Override
  protected final boolean lessThan(ScoreDoc hitA, ScoreDoc hitB) {
    if (hitA.score == hitB.score)
      return hitA.doc > hitB.doc; 
    else
      return hitA.score < hitB.score;
  }

}
