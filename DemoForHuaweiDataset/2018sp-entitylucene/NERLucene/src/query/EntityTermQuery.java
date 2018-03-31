package query;


import java.io.IOException;
import java.util.Objects;
import java.util.Set;

import org.apache.lucene.index.IndexReaderContext;
import org.apache.lucene.index.LeafReader;
import org.apache.lucene.index.LeafReaderContext;
import org.apache.lucene.index.PostingsEnum;
import org.apache.lucene.index.ReaderUtil;
import org.apache.lucene.index.Term;
import org.apache.lucene.index.TermContext;
import org.apache.lucene.index.TermState;
import org.apache.lucene.index.Terms;
import org.apache.lucene.index.TermsEnum;
import org.apache.lucene.search.CollectionStatistics;
import org.apache.lucene.search.Explanation;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.Scorer;
import org.apache.lucene.search.TermStatistics;
import org.apache.lucene.search.Weight;
import org.apache.lucene.search.similarities.Similarity;
import org.apache.lucene.search.similarities.Similarity.SimScorer;


public class EntityTermQuery extends Query {

  private final Term term;
  private final TermContext perReaderTermState;

  final class EntityWeight extends Weight {
    private final Similarity similarity;
    private final Similarity.SimWeight stats;
    private final TermContext termStates;
    private final boolean needsScores;

    public EntityWeight(IndexSearcher searcher, boolean needsScores,
        float boost, TermContext termStates) throws IOException {
      super(EntityTermQuery.this);
      if (needsScores && termStates == null) {
        throw new IllegalStateException("termStates are required when scores are needed");
      }
      this.needsScores = needsScores;
      this.termStates = termStates;
      this.similarity = searcher.getSimilarity(needsScores);

      final CollectionStatistics collectionStats;
      final TermStatistics termStats;
      if (needsScores) {
        collectionStats = searcher.collectionStatistics(term.field());
        termStats = searcher.termStatistics(term, termStates);
      } else {
        final int maxDoc = searcher.getIndexReader().maxDoc();
        collectionStats = new CollectionStatistics(term.field(), maxDoc, -1, -1, -1);
        termStats = new TermStatistics(term.bytes(), maxDoc, -1);
      }
     
      this.stats = similarity.computeWeight(boost, collectionStats, termStats);
    }

    @Override
    public void extractTerms(Set<Term> terms) {
      terms.add(getTerm());
    }

    @Override
    public String toString() {
      return "weight(" + EntityTermQuery.this + ")";
    }

    /**
     * Create scorer in this function.
     * @author jjiang1110@gmail.com
     */
    @Override
    public EntityScorer scorer(LeafReaderContext context) throws IOException {
      assert termStates == null || termStates.wasBuiltFor(ReaderUtil.getTopLevelContext(context)) : "The top-reader used to create Weight is not the same as the current reader's top-reader (" + ReaderUtil.getTopLevelContext(context);;
      final TermsEnum termsEnum = getTermsEnum(context);
      if (termsEnum == null) {
        return null;
      }
      PostingsEnum docs = termsEnum.postings(null, PostingsEnum.ALL);
      assert docs != null;
      return new EntityScorer(this, docs, similarity.simScorer(stats, context), type, mode, window);
    }

    private TermsEnum getTermsEnum(LeafReaderContext context) throws IOException {
      if (termStates != null) {
        assert termStates.wasBuiltFor(ReaderUtil.getTopLevelContext(context)) : "The top-reader used to create Weight is not the same as the current reader's top-reader (" + ReaderUtil.getTopLevelContext(context);
        final TermState state = termStates.get(context.ord);
        if (state == null) { // term is not present in that reader
          assert termNotInReader(context.reader(), term) : "no termstate found but term exists in reader term=" + term;
          return null;
        }
        final TermsEnum termsEnum = context.reader().terms(term.field()).iterator();
        termsEnum.seekExact(term.bytes(), state);
        return termsEnum;
      } else {
        Terms terms = context.reader().terms(term.field());
        if (terms == null) {
          return null;
        }
        final TermsEnum termsEnum = terms.iterator();
        if (termsEnum.seekExact(term.bytes())) {
          return termsEnum;
        } else {
          return null;
        }
      }
    }

    private boolean termNotInReader(LeafReader reader, Term term) throws IOException {
      return reader.docFreq(term) == 0;
    }

    @Override
    public Explanation explain(LeafReaderContext context, int doc) throws IOException {
      Scorer scorer = scorer(context);
      if (scorer != null) {
        int newDoc = scorer.iterator().advance(doc);
        if (newDoc == doc) {
          float freq = scorer.freq();
          SimScorer docScorer = similarity.simScorer(stats, context);
          Explanation freqExplanation = Explanation.match(freq, "termFreq=" + freq);
          Explanation scoreExplanation = docScorer.explain(doc, freqExplanation);
          return Explanation.match(
              scoreExplanation.getValue(),
              "weight(" + getQuery() + " in " + doc + ") ["
                  + similarity.getClass().getSimpleName() + "], result of:",
              scoreExplanation);
        }
      }
      return Explanation.noMatch("no matching term");
    }
  }

  public EntityTermQuery(Term t) {
    term = Objects.requireNonNull(t);
    perReaderTermState = null;
  }

  

  @Override
  public Weight createWeight(IndexSearcher searcher, boolean needsScores, float boost) throws IOException {
    final IndexReaderContext context = searcher.getTopReaderContext();
    final TermContext termState;
    if (perReaderTermState == null
        || perReaderTermState.wasBuiltFor(context) == false) {
      if (needsScores) {
        termState = TermContext.build(context, term);
      } else {
        termState = null;
      }
    } else {
      termState = this.perReaderTermState;
    }

    return new EntityWeight(searcher, needsScores, boost, termState);
  }

  @Override
  public String toString(String field) {
    StringBuilder buffer = new StringBuilder();
    if (!term.field().equals(field)) {
      buffer.append(term.field());
      buffer.append(":");
    }
    buffer.append(term.text());
    return buffer.toString();
  }

  @Override
  public boolean equals(Object other) {
    return sameClassAs(other) &&
           term.equals(((EntityTermQuery) other).term);
  }

  @Override
  public int hashCode() {
    return classHash() ^ term.hashCode();
  }

  /**
   * @author jjiang1110@gmail.com
   */
  private String type;
  private String mode;
  private int window;
  
  public EntityTermQuery(Term t, String type, String mode, int window){
	  this(t);
	  this.type = type;
	  this.mode = mode;
	  this.window = window;
  }
 
  public EntityTermQuery(Term t, TermContext states) {
    assert states != null;
    term = Objects.requireNonNull(t);
    perReaderTermState = Objects.requireNonNull(states);
  }

  public Term getTerm() {
    return term;
  }
  
}
