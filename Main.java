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
import org.json.simple.JSONArray;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

public class Main {

	

	public static void main(String[] args) {
		
		
		Map<String, Analyzer> analyzerPerField = new HashMap<>();
//		analyzerPerField.put("TextForTopic", new StandardAnalyzer());
//		analyzerPerField.put("TextForDescription", new StandardAnalyzer());
		analyzerPerField.put("Text", new StandardAnalyzer());
		analyzerPerField.put("Day", new StandardAnalyzer());
		PerFieldAnalyzerWrapper analyzer = new PerFieldAnalyzerWrapper(new StandardAnalyzer(), analyzerPerField);
		
		
		
		IndexerClassIncr indIncr = new IndexerClassIncr();
		
		indIncr.indexerAndQueiesController(analyzer, null);
		
		
		
		
//		Analyzer analyzer = new StandardAnalyzer();
		
//		IndexerClass indexer = new IndexerClass();
		
//		indexer.openIndex(analyzer, null);
//		indexer.indexDocuments();
//		indexer.close();
		
//		indexer.indexSearch(analyzer, null);
		
		System.out.println("Done");
		
		
		  

	}

}
