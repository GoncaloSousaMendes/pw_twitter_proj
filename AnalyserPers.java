import java.io.Reader;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;

import org.apache.lucene.analysis.CharArraySet;
import org.apache.lucene.analysis.LowerCaseFilter;
import org.apache.lucene.analysis.StopFilter;
import org.apache.lucene.analysis.StopwordAnalyzerBase;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.commongrams.CommonGramsFilter;
import org.apache.lucene.analysis.ngram.EdgeNGramTokenFilter;
import org.apache.lucene.analysis.ngram.NGramTokenFilter;
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
	
	
	//Standard;Lower;Stop;Shingle;Common;NGramToken;EdgeNGram;Snowball
	// String containing the analysers to use
	private String analysers;


	/**
	 * Builds an analyzer with the default stop words ({@link #STOP_WORDS_SET}).
	 */
	public AnalyserPers(String analysers) {
		super(stopSet);
		this.analysers = analysers;
	}

	@Override
	protected TokenStreamComponents createComponents(final String fieldName) {

		
		// THE FIELD IS IGNORED 
		// ___BUT___ 
		// you can provide different TokenStremComponents according to the fieldName
		
		final StandardTokenizer src = new StandardTokenizer();

		
		TokenStream tok = null;
		
		String [] analyserToUse = analysers.split(";");

		boolean stand = false;
		boolean lower = false;
		boolean stop = false;
		boolean shingle = false;
		boolean commom = false;
		boolean ngram = false;
		boolean edge = false;
		boolean snowball = false;

		for (String a: analyserToUse){
			
			if (a.equals("Standard"))
				stand = true;
			
			else if (a.equals("Lower"))
				lower = true;
			
			else if (a.equals("Stop"))
				stop = true;
			
			else if (a.equals("Shingle"))
				shingle = true;
			
			else if (a.equals("Common"))
				commom = true;
			
			else if (a.equals("NGramToken"))
				ngram = true;
			
			else if (a.equals("EdgeNGram"))
				edge = true;
			
			else if (a.equals("Snowball"))
				snowball = true;
			
		}
		
		if (!stand && !lower && !stop && !shingle && !commom && !ngram && !edge && !snowball){
			System.out.println("Invalid analysers");
			System.exit(0);
		}
		
		
		if (stand)
			tok = new StandardFilter(src);
		
		if (lower && tok != null)
			tok = new LowerCaseFilter(tok);
		else if(lower && tok == null)
			tok = new LowerCaseFilter(src);
		
		if (stop && tok != null)
			tok = new StopFilter(tok, stopwords);
		else if(stop && tok == null)
			tok = new StopFilter(src, stopwords);
		
		if (shingle && tok != null)
			tok = new ShingleFilter(tok, 2, 3);
		else if(shingle && tok == null)
			tok = new ShingleFilter(src);
		
		if (commom && tok != null)
			tok = new CommonGramsFilter(tok, stopwords);
		else if(commom && tok == null)
			tok = new CommonGramsFilter(src, stopwords);
		
		if (ngram && tok != null)
			tok = new NGramTokenFilter(tok,2,5);
		else if(ngram && tok == null)
			tok = new NGramTokenFilter(src,2,5);
		
		if (edge && tok != null)
			tok = new EdgeNGramTokenFilter(tok,2,5);
		else if(edge && tok == null)
			tok = new EdgeNGramTokenFilter(src,2,5);
		
		if (snowball && tok != null)
			tok = new SnowballFilter(tok, "English");
		else if(snowball && tok == null)
			tok = new SnowballFilter(src, "English");
		
		
//		tok = new StandardFilter(src);					// text into non punctuated text
//		tok = new LowerCaseFilter(tok);					// changes all text into lowercase
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
