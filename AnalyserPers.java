import java.io.Reader;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;

import org.apache.lucene.analysis.CharArraySet;
import org.apache.lucene.analysis.LowerCaseFilter;
import org.apache.lucene.analysis.StopFilter;
import org.apache.lucene.analysis.StopwordAnalyzerBase;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.Analyzer.TokenStreamComponents;
import org.apache.lucene.analysis.shingle.ShingleFilter;
import org.apache.lucene.analysis.snowball.SnowballFilter;
import org.apache.lucene.analysis.standard.StandardFilter;
import org.apache.lucene.analysis.standard.StandardTokenizer;

public class AnalyserPers extends StopwordAnalyzerBase {
	
	private static final Pattern REMOVE_TAGS = Pattern.compile("<.+?>");

	/**
	 * An unmodifiable set containing some common English words that are not
	 * usually useful for searching.
	 */
	static List<String> stopWords = Arrays.asList("a", "an", "and", "are", "as", "at", "be", "but", "by", "for", "if",
			"in", "into", "is", "it", "no", "not", "of", "on", "or", "such", "that", "the", "their", "then", "there",
			"these", "they", "this", "to", "was", "will", "with", "it's");
	static CharArraySet stopSet = new CharArraySet(stopWords, false);

	/** Default maximum allowed token length */
	private int maxTokenLength = 25;

	/**
	 * Builds an analyzer with the default stop words ({@link #STOP_WORDS_SET}).
	 */
	public AnalyserPers() {
		super(stopSet);
	}

	@Override
	protected TokenStreamComponents createComponents(final String fieldName) {

		
		// THE FIELD IS IGNORED 
		// ___BUT___ 
		// you can provide different TokenStremComponents according to the fieldName
		
		final StandardTokenizer src = new StandardTokenizer();
		
		TokenStream tok = null;
		tok = new StandardFilter(src);					// text into non punctuated text
//		tok = new LowerCaseFilter(src);					// changes all text into lowercase
//		tok = new StopFilter(tok, stopwords);			// removes stop words

//		tok = new ShingleFilter(tok, 2, 3);				// creates word-grams with neighboring works
//		tok = new CommonGramsFilter(tok, stopwords);	// creates word-grams with stopwords
		
//		tok = new NGramTokenFilter(tok,2,5);			// creates unbounded n-grams 
//		tok = new EdgeNGramTokenFilter(src,2,5);		// creates word-bounded n-grams
	
//		tok = new SnowballFilter(tok, "English");		// stems words according to the specified language
		
		return new TokenStreamComponents(src, tok) {
			@Override
			protected void setReader(final Reader reader) {
				src.setMaxTokenLength(AnalyserPers.this.maxTokenLength);
				//super.setReader(new HTMLStripCharFilter(reader));
				super.setReader(reader);
			}
		};
	}
	

	@Override
	protected TokenStream normalize(String fieldName, TokenStream in) {
		TokenStream result = new StandardFilter(in);
		result = new LowerCaseFilter(result);
		return result;
	}

}
