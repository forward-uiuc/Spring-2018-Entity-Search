package query;

import java.io.IOException;

import org.apache.lucene.index.FieldInvertState;
import org.apache.lucene.index.LeafReaderContext;
import org.apache.lucene.search.CollectionStatistics;
import org.apache.lucene.search.TermStatistics;
import org.apache.lucene.search.similarities.Similarity;
import org.apache.lucene.util.BytesRef;

public class EntitySimilarity extends Similarity{

	@Override
	public long computeNorm(FieldInvertState state) {
		return 0;
	}

	@Override
	public SimWeight computeWeight(float boost, CollectionStatistics collectionStats, TermStatistics... termStats) {
		return null;
	}

	@Override
	public SimScorer simScorer(SimWeight weight, LeafReaderContext context) throws IOException {
		return new EntitySimScorer();
	}
	
	public class EntitySimScorer extends SimScorer{
		
		/**
		 * EntityScorer will call this function.
		 * @param pos
		 * @param epos
		 * @return
		 * @throws IOException
		 */
		public float score(int pos, int epos) throws IOException {
			float numerator = 1.0f;
			float denominator = Math.abs(pos - epos);
			return numerator / denominator;
		}

		@Override
		public float score(int doc, float freq) throws IOException {
			return 0;
		}

		@Override
		public float computeSlopFactor(int distance) {
			return 0;
		}

		@Override
		public float computePayloadFactor(int doc, int start, int end, BytesRef payload) {
			return 0;
		}
		
	}
	
	private class EntitySimWeight extends SimWeight{
		
	}
	

}
