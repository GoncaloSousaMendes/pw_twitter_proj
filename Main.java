//import java.io.BufferedReader;
//import java.io.FileReader;
//import java.io.IOException;
//import java.util.Iterator;
//import java.util.Set;

import java.io.FileReader;
import java.util.HashMap;
import java.util.Map;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.miscellaneous.PerFieldAnalyzerWrapper;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.search.similarities.BM25Similarity;
import org.apache.lucene.search.similarities.Similarity;
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

		AnalyserPers analyzerInField = new AnalyserPers();
		StandardAnalyzer ana = new StandardAnalyzer();
		analyzerPerField.put("Text", ana);
		analyzerPerField.put("Day", ana);
		PerFieldAnalyzerWrapper analyzer = new PerFieldAnalyzerWrapper(new StandardAnalyzer(), analyzerPerField);
		
		Similarity similarity = null;
//		similarity = new BM25Similarity();
//		similarity = new LMDirichletSimilarity();
//		similarity = new TFIDFSimilarity();
		
		// Incremental
//		IndexerClassIncr indIncr = new IndexerClassIncr();
//		indIncr.indexerAndQueriesController(analyzer, similarity);
		
		//indexação toda junta
		IndexerClass indexer = new IndexerClass();
		
//		indexer.openIndex(analyzer, similarity);
//		indexer.indexDocuments();
//		indexer.close();
//		
		indexer.indexSearch(analyzer, similarity);
		
		
		long finalTime = System.currentTimeMillis();
		System.out.println("Done");
		System.out.println("Time: " + (finalTime - intialTime));
		
		
		  

	}

}
