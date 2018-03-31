package query;

import java.util.LinkedHashMap;
import java.util.LinkedList;

import org.apache.lucene.search.ScoreDoc;

import formatting.TokenField;
import ner.annotation.EntityAnnotation;

/**
 * Regard entities as documents.
 * @author jjiang1110@gmail.com
 */

public class EntityScoreDoc extends ScoreDoc{
	
	public EntityAnnotation entity;
	
	public EntityScoreDoc(int doc, float score) {
		super(doc, score);
	}
	
	public EntityScoreDoc(int doc, float score, EntityAnnotation entity) {
		super(doc, score);
		this.entity = entity;
	}

}
