package query;


import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.LinkedList;

import org.apache.lucene.index.PostingsEnum;
import org.apache.lucene.search.DocIdSetIterator;
import org.apache.lucene.search.Scorer;
import org.apache.lucene.search.Weight;
import org.apache.lucene.search.similarities.BM25Similarity;
import org.apache.lucene.search.similarities.Similarity;
import org.apache.lucene.util.BytesRef;

import formatting.EntityTokenFormatter;
import formatting.TokenField;


final class EntityScorer extends Scorer {
  private final PostingsEnum postingsEnum;
  private final Similarity.SimScorer docScorer;  

  @Override
  public int docID() {
    return postingsEnum.docID();
  }

  @Override
  public int freq() throws IOException {
    return postingsEnum.freq();
  }

  @Override
  public DocIdSetIterator iterator() {
    return postingsEnum;
  }

  @Override
  public float score() throws IOException {
    assert docID() != DocIdSetIterator.NO_MORE_DOCS;
    return docScorer.score(postingsEnum.docID(), postingsEnum.freq());
  }
  
  @Override
  public String toString() { 
	  return "scorer(" + weight + ")[" + super.toString() + "]"; 
  }
  
  /**
   * @author jjiang1110@gmail.com
   */
  private int pos;
  private String type;
  private String mode;
  private int window;
  
  EntityScorer(Weight weight, PostingsEnum td, Similarity.SimScorer docScorer, String type, String mode, int window) {
    super(weight);
    this.docScorer = docScorer;
    this.postingsEnum = td;
    this.type = type;
    this.mode = mode;
    this.window = window;
  }
  
  public String getType(){
	  return type;
  }
  
  public int getPosition(){
	  return pos;
  }
  
  public int getWindow(){
	  return window;
  }
  
  /**
   * Give a single entity score. Call EntitySimilarity class.
   * @param pos
   * @param epos
   * @return
   * @throws IOException
   */
  public float score(int pos, int epos) throws IOException {
	  assert docID() != DocIdSetIterator.NO_MORE_DOCS;
	  return ((EntitySimilarity.EntitySimScorer)docScorer).score(pos, epos);
  }
  
  /**
   * Get entities from each position of a document.
   * @return
   * @throws ClassNotFoundException
   * @throws IOException
   */
  public LinkedList<EntityTmp> getEntity() throws ClassNotFoundException, IOException{
	  EntityTokenFormatter eiv = new EntityTokenFormatter(mode);
	  LinkedList<EntityTmp> ll = new LinkedList();
	  for(int i = 0; i < postingsEnum.freq(); i++){
			pos = postingsEnum.nextPosition();
			LinkedHashMap<String, TokenField> lhm = eiv.getTokenContent().deserialize(postingsEnum.getPayload().bytes);
			EntityTmp tmp = new EntityTmp(lhm, pos);
			ll.add(tmp);
	  }
	  return ll;
  }

  
}
