import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.io.Writer;
import java.nio.file.Paths;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.IntPoint;
import org.apache.lucene.document.LongPoint;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.index.Term;
import org.apache.lucene.index.IndexWriterConfig.OpenMode;
import org.apache.lucene.queryparser.classic.MultiFieldQueryParser;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.BooleanClause;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.search.similarities.Similarity;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;


public class IndexerClassIncr {
	
	private String tweetsPath = "src/tweets/rts2016-qrels-tweets2016.jsonl";
	private String indexPath = "src/index";
//	private String indexPath = "C:/Users/aluca_000/Google Drive/FCT/16-17/2º Semestre/Java/Ind Proj PW/src/index";

	private String topicPath = "src/profiles/TREC2016-RTS-topics.json";

	private boolean create = true;

	private IndexWriter idx;
	
	// save the list containg the tweets by day
	private Map <String,List <JSONObject>> listOfTweetsByDays;
	private String baseNameList = "tweetsDay";
	String [] days;
	
	public IndexerClassIncr(){

		listOfTweetsByDays = new HashMap <String,List <JSONObject>> (30);

		listOfTweetsByDays.put("tweetsDay02", new LinkedList <JSONObject> ());
		listOfTweetsByDays.put("tweetsDay03", new LinkedList <JSONObject> ());
		listOfTweetsByDays.put("tweetsDay04", new LinkedList <JSONObject> ());
		listOfTweetsByDays.put("tweetsDay05", new LinkedList <JSONObject> ());
		listOfTweetsByDays.put("tweetsDay06", new LinkedList <JSONObject> ());
		listOfTweetsByDays.put("tweetsDay07", new LinkedList <JSONObject> ());
		listOfTweetsByDays.put("tweetsDay08", new LinkedList <JSONObject> ());
		listOfTweetsByDays.put("tweetsDay09", new LinkedList <JSONObject> ());
		listOfTweetsByDays.put("tweetsDay10", new LinkedList <JSONObject> ());
		listOfTweetsByDays.put("tweetsDay11", new LinkedList <JSONObject> ());
		
		days = new String [10];
		days[0] = "02";
		days[1] = "03";
		days[2] = "04";
		days[3] = "05";
		days[4] = "06";
		days[5] = "07";
		days[6] = "08";
		days[7] = "09";
		days[8] = "10";
		days[9] = "11";
		
		
		// Get all the tweets and save them to be incrementaly indexed
		JSONParser parser = new JSONParser();
		try (BufferedReader br = new BufferedReader(new FileReader(tweetsPath))) {

			String line = br.readLine(); // The first line is dummy
			Object obj;
			while(line != null){
				obj = parser.parse(line);
				JSONObject tweet = (JSONObject) obj;
				// Extract Date
				String date = (String) tweet.get("created_at");
				// Extract the specific date
				String [] date_sep = date.split(" ");
				//Extract the day
				String day = date_sep[2];
				// The name of the list in the hashMap
				String listName = baseNameList + day;

				if (listOfTweetsByDays.containsKey(listName))
					listOfTweetsByDays.get(listName).add(tweet);
//				else
//					System.out.println("No list to add " + date);

				line = br.readLine();
			}
			
		} catch (IOException e) {

			e.printStackTrace();
		} catch (org.json.simple.parser.ParseException e) {

			e.printStackTrace();
		}	
	}
	
	
	/**
	 * @param analyzer
	 * @param similarity
	 * This class controls the incremental build of the indexer and the quering
	 * first creates and indexer, then, day by day, adds the tweets and performs the queries
	 */
	public void indexerAndQueriesController(Analyzer analyzer, Similarity similarity){
		
	// create a new indexer, the variable CREATE is true
	System.out.println("Create index");
	this.openIndex(analyzer, similarity);
	this.close();
	
	
		
	// this is needed, we don't want to create new indexer for each day
	create = false;
	
	String listName = "";
	
	Writer writer = null;
	String submissionName = "results.txt";
	try {
		writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(submissionName), "utf-8"));

	} catch (UnsupportedEncodingException e) {
		e.printStackTrace();
	} catch (FileNotFoundException e) {
		e.printStackTrace();
	}
	

	for (String day : days){
		// first index the tweets
		// open the index
		this.openIndex(analyzer, similarity);
		// get the name of the list
		listName = baseNameList + day;
		// index the tweets
		indexDocuments(listOfTweetsByDays.get(listName));
		//close index
		this.close();
		// then perform the queries
		indexSearch(writer, analyzer, similarity, day);
	}
	
	
	try {
		writer.close();
		} catch (Exception ex) {
			ex.printStackTrace();
		}

	}
	

	public void openIndex(Analyzer analyzer, Similarity similarity) {

		try {
			IndexWriterConfig iwc = new IndexWriterConfig(analyzer);
			if (similarity != null)
				iwc.setSimilarity(similarity);
			if (create) {
				// Create a new index, removing any
				// previously indexed documents:
				iwc.setOpenMode(OpenMode.CREATE);
			} else {
				// Add new documents to an existing index:
				iwc.setOpenMode(OpenMode.CREATE_OR_APPEND);
			}

			// ====================================================
			// Open/create the index in the specified location
			Directory dir = FSDirectory.open(Paths.get(indexPath));
			idx = new IndexWriter(dir, iwc);

		} catch (IOException e) {
			e.printStackTrace();
		}

	}
	
	public void indexDocuments(List<JSONObject> tweetsByDay) {
		if (idx == null)
			return;
		System.out.println("Indexing documents...");
		// ====================================================
		// Parse the Answers data
		
		
		for (JSONObject tweet: tweetsByDay){
//			JSONObject tweet = (JSONObject) obj;
			indexDoc(tweet);
		}

	}

	private void indexDoc(JSONObject tweet) {

		Document doc = new Document();
		String id = "0";
		try {
			
			
			// Extract field Id
			id = (String) tweet.get("id_str");
			doc.add(new TextField("Id", id, Field.Store.YES));
			
			// Extract Date
			String date = (String) tweet.get("created_at");
			doc.add(new TextField("Date", date, Field.Store.YES));
			
			// Extract the specific dates
			String [] date_sep = date.split(" ");
			doc.add(new TextField("Day", date_sep[2], Field.Store.YES));
			
			
			// Extract hastags
			String tags = "";
			JSONObject entities = (JSONObject) tweet.get("entities");
			JSONArray hashtags = (JSONArray) entities.get("hashtags");
			for (Object o: hashtags){
					JSONObject hash = (JSONObject) o;
					tags += hash.get("text") + ";";
			}
//			System.out.println(tags);
			doc.add(new TextField("Hashtags", tags, Field.Store.YES));

			// Extract field Body
			String body = (String) tweet.get("text");
			//retirar tags de html
//			body = body.replaceAll("\\<[^>]*>","");
			doc.add(new TextField("Text", body, Field.Store.YES));
			// ====================================================
			// Add the document to the index
			if (idx.getConfig().getOpenMode() == OpenMode.CREATE) {

				idx.addDocument(doc);

			} else if (idx.getConfig().getOpenMode() == OpenMode.CREATE_OR_APPEND) {
				idx.addDocument(doc);
//				idx.updateDocument(new Term("Id", id.toString()), doc);
			}
		} catch (IOException e) {
			System.out.println("Error adding document " + id);
			e.printStackTrace();
		} catch (Exception e) {
			System.err.println("Error parsing document " + id);
			e.printStackTrace();
			System.exit(0);
		}
	}

	public void indexSearch(Writer writer, Analyzer analyzer, Similarity similarity, String day) {
		System.out.println("Quering and results...");
		//The index reader
		IndexReader reader = null;

		int numberOfTweets = 100;
		boolean debug = false;

		try {

			//fetch index in the directory provided
			reader = DirectoryReader.open(FSDirectory.open(Paths.get(indexPath)));
			//initialize the searcher and the analyzer (the same one used to write the index)
			IndexSearcher searcher = new IndexSearcher(reader);
			
			if (similarity != null)
				searcher.setSimilarity(similarity);

			//parser for the queries
			/*
			 * Para guardar as queries
			 * Na posição 0 -> o dia
			 * Na posição 1 -> o titulo
			 * Na posição 2 -> a descrição
			 */
			String[] queryL = new String[3];

			String[] fields = {"Day", "Text", "Text" };
			BooleanClause.Occur[] flags = {BooleanClause.Occur.MUST, BooleanClause.Occur.MUST, BooleanClause.Occur.MUST};
			
			Query query = null;
			
			String runTag = "";

			try {
				JSONParser parser = new JSONParser();
				
				JSONArray topics = (JSONArray) parser.parse(new FileReader(topicPath));
				
				String topicTitle = "";
				String topicId = "";
				
				int i = 0;

				//					System.out.println("/////---------------------------------------------------///////");
				//					System.out.println("Day: " + day);
				//So para um dia
				runTag = "#run" + String.valueOf(i);
				i++;
				for (Object topicObject : topics){

					JSONObject topic = (JSONObject) topicObject;
					topicId = (String) topic.get("topid");
					queryL[0] = day;
					topicTitle = (String)  topic.get("title");
					queryL[1] = topicTitle;
					queryL[2] = (String)  topic.get("description");


					try {
						query = MultiFieldQueryParser.parse(queryL, fields, flags, analyzer);
					} catch (org.apache.lucene.queryparser.classic.ParseException e) {
						queryL[1] = queryL[1].replace("\"", "");
						queryL[2] = queryL[2].replace("\"", "");

						try {
							query = MultiFieldQueryParser.parse(queryL, fields, flags, analyzer);
						} catch (org.apache.lucene.queryparser.classic.ParseException e2) {
							//System.out.println("Error parsing query string.");
							//System.out.println(topicId);
							//System.out.println(queryL[1]);
							//System.out.println(queryL[2]);
							e2.printStackTrace();
						}
					}


					//						//look on the index, returning the top 100 answers

					TopDocs results = searcher.search(query, numberOfTweets);
					ScoreDoc[] hits = results.scoreDocs;
					int numTotalHits = results.totalHits;

					if (debug){
						System.out.println("-------------------------------------------------------------------------------------------");
						System.out.println(topicTitle + " " + topicId);
						System.out.println(numTotalHits + " total matching documents");
					}


					//iterate through the answers 
					for (int j = 0; j < numberOfTweets && j < numTotalHits; j++) {

						Document doc = searcher.doc(hits[j].doc);
						String tweetId = doc.get("Id");
						String date = doc.get("Date");



						String[] dd = date.split(" ");


						String dateToWrite = dd[5] + "08" + dd[2];

						float score = hits[j].score;

						if (debug){
							String text = doc.get("Text");
							String hashtags = doc.get("Hashtags");
							System.out.println(date);
							System.out.println(text);
							System.out.println(hashtags);
						}
						if (!writeToFile(writer, dateToWrite, topicId, tweetId, (j+1), score, runTag)){
							System.out.println("Erro wrinting in file");
							System.exit(0);
						}
						

					}
				} 


			} catch (org.json.simple.parser.ParseException e) {
						
						e.printStackTrace();
			} catch( Exception general){
				general.printStackTrace();
			}
			
		} catch (Exception general2){
			general2.printStackTrace();
		}
		
		finally {
//			try {
//				writer.close();
//				} catch (Exception ex) {
//					ex.printStackTrace();
//					}
		}


	}

	
	protected void close() {
		try {
			idx.close();
		} catch (IOException e) {
			System.out.println("Error closing the index.");
		}
	}

	private boolean writeToFile(Writer writer, String date, String topic_id, String tweet_id, int rank, float score, String runTag){
		//writting format: YYYYMMDD topic_id Q0 tweet_id rank score runtag
		try{
			writer.write(date + "\t" + topic_id + "\t" + "0" + "\t" + tweet_id + "\t"+ rank +"\t" + score + "\t" + runTag + "\n");

			writer.flush();

		}catch (IOException e1) {
			return false;
		}

		return true;
	}



}
