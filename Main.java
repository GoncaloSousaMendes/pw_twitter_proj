//import java.io.BufferedReader;
//import java.io.FileReader;
//import java.io.IOException;
//import java.util.Iterator;
//import java.util.Set;

import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.miscellaneous.PerFieldAnalyzerWrapper;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.search.similarities.BM25Similarity;
import org.apache.lucene.search.similarities.ClassicSimilarity;
import org.apache.lucene.search.similarities.LMDirichletSimilarity;
import org.apache.lucene.search.similarities.Similarity;
import org.apache.lucene.search.similarities.TFIDFSimilarity;
import org.json.simple.JSONArray;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

public class Main {


	public static void main(String[] args) {
		
		
		long intialTime = System.currentTimeMillis();
		Map<String, Analyzer> analyzerPerField = new HashMap<>();
		
		// Mudar a variavel para fazer diferentes runs e guardar com nomes diferentes
		
		if(args.length != 5){
			System.out.println("Not enough arguments");
			System.out.println("Usage: int numbeOfTheRun String analysers int gramSize String similarities boolean UseUserScore");
			System.out.println("Possible analysers: Standard;Lower;Stop;Shingle;Common;NGramToken;EdgeNGram;Snowball;");
			System.out.println("Possible similarities: BM25;LMD;Classic;");
			System.out.println("Only one similarity per run");
			
			System.exit(0);
		}
		
		String run = args[0];
		//Standard;Lower;Stop;Shingle;Common;NGramToken;EdgeNGram;Snowball
		String analysers = args[1];
		int gramSize =  Integer.valueOf(args[2]);
		String sim =  args[3];
		boolean userScore = false;
		if(args[4].equals("true") || args[4].equals("false"))
			userScore =  Boolean.valueOf(args[4]);
		else{
			System.out.println("Invalid boolean argument: use 'true' or 'false'");
		}

		String runTag = "run" + run;


		AnalyserPers analyzerInField = new AnalyserPers(analysers, gramSize);
		StandardAnalyzer ana = new StandardAnalyzer();
		analyzerPerField.put("Text", ana);
		analyzerPerField.put("Hashtags", ana);
		analyzerPerField.put("Day", ana);
		PerFieldAnalyzerWrapper analyzer = new PerFieldAnalyzerWrapper(new StandardAnalyzer(), analyzerPerField);
		
		
		
		Similarity similarity = null;
		if (sim.equals("BM25"))
			similarity = new BM25Similarity();
		else if(sim.equals("LMD"))
			similarity = new LMDirichletSimilarity();
		else if(sim.equals("Classic"))
			similarity = new ClassicSimilarity(); //TFIDF
		
		// Incremental
//		IndexerClassIncr indIncr = new IndexerClassIncr();
//		indIncr.indexerAndQueriesController(analyzer, similarity);
		
		Indexer indexer = null;
		
//		indexer = new IndexerBase();
		indexer = new IndexerHashtags();
		
		
		indexer.openIndex(analyzer, similarity);
		indexer.indexDocuments();
		indexer.close();
		
		indexer.indexSearch(analyzer, similarity, runTag, userScore);
		
		
		//indexação toda junta
//		IndexerClass indexer = new IndexerClass();
//		
//		indexer.openIndex(analyzer, similarity);
//		indexer.indexDocuments();
//		indexer.close();
//
//		
//		indexer.indexSearch(analyzer, similarity, runTag, userScore);
		
		
		long finalTime = System.currentTimeMillis();
		
		System.out.println("\nTime: " + (finalTime - intialTime));
		System.out.println("Computing results");
		String nameFile = runTag + ".txt";
		
		try{    
		    Process p = Runtime.getRuntime().exec(" cmd /c run_eval.bat " + nameFile);
		    p.waitFor();

		}catch( IOException ex ){
		    //Validate the case the file can't be accesed (not enought permissions)

		}catch( InterruptedException ex ){
		    //Validate the case the process is being stopped by some external situation     

		}
		
		
		System.out.println("Done");

	}

}
