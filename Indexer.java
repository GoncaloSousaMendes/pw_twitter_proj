import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.search.similarities.Similarity;

/**
 * 
 */

/**
 * @author Moncada
 *
 */
public interface Indexer {
	
	void openIndex(Analyzer analyzer, Similarity similarity);
	
	void indexDocuments();
	
	void indexSearch(Analyzer analyzer, Similarity similarity, String runTag, boolean userScore);
	
	void close();

}
