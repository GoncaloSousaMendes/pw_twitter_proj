import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.file.Paths;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
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


public class IndexerClass {
	
	private String tweetsPath = "src/tweets/rts2016-qrels-tweets2016.jsonl";
	private String indexPath = "src/index";

//	private String topicPath = "src/profiles/TREC2016-RTS-topics.json";
//	private String topicPath = "src/profiles/TREC2015-MB-eval-topics.json";
//	private String topicPath = "src/profiles/TREC2015-MB-noeval-topics-culled.json";
	private String topicPath = "src/profiles/pw_top_10_topics.json";
	
	
	private UserRank ranksForUsers;
	
	//save the tweets, to find repetitions
	static Map <String, JSONObject> tweetsMap;
	
	private boolean create = true;

	private IndexWriter idx;
	
	public IndexerClass(){
		tweetsMap = new HashMap<String, JSONObject>();
	}
	
	public void openIndex(Analyzer analyzer, Similarity similarity) {

		try {
			System.out.println("Creating and openning index...");
			// ====================================================
			// Select the data analyser to tokenise document data
			//analyzer = new StandardAnalyzer();

			// ====================================================
			// Configure the index to be created/opened
			//
			// IndexWriterConfig has many options to be set if needed.
			//
			// Example: for better indexing performance, if you
			// are indexing many documents, increase the RAM
			// buffer. But if you do this, increase the max heap
			// size to the JVM (eg add -Xmx512m or -Xmx1g):
			//
			// iwc.setRAMBufferSizeMB(256.0);
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
	
	public void indexDocuments() {
		ranksForUsers = new UserRank();
		if (idx == null)
			return;
		System.out.println("Indexing documents...");
		// ====================================================
		// Parse the Answers data
		JSONParser parser = new JSONParser();
		try (BufferedReader br = new BufferedReader(new FileReader(tweetsPath))) {

			String line = br.readLine(); // The first line is dummy
			Object obj;
			while(line != null){
				obj = parser.parse(line);
				JSONObject tweet = (JSONObject) obj;
				indexDoc(tweet);
				//Adicionar o user
				ranksForUsers.saveUser( (JSONObject) tweet.get("user"));
				line = br.readLine();
			}
			
			
		} catch (IOException e) {

			e.printStackTrace();
		} catch (org.json.simple.parser.ParseException e) {

			e.printStackTrace();
		}
		
//		System.out.println("Max followers: " + ranksForUsers.getmaxFollowers());
//		System.out.println("Number of users: " + ranksForUsers.getNumberOfUsers());
		
		ranksForUsers.scoreUsers();
	}

	private void indexDoc(JSONObject tweet) {

		Document doc = new Document();
		
		String id = "0";
		
		try {
			
			
			
			// Extract field Id
			id = (String) tweet.get("id_str");
			
			//see if the tweet already exists
			if(!tweetsMap.containsKey(id))
				tweetsMap.put(id, tweet);
			else {
				//we don't need repeated tweets
				return;
			}
			
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
			doc.add(new TextField("Hashtags", tags, Field.Store.YES));

			// Extract field Body
			String body = (String) tweet.get("text");
			//retirar tags de html
//			body = body.replaceAll("\\<[^>]*>","");
			doc.add(new TextField("Text", body, Field.Store.YES));
//			doc.add(new TextField("TextForTitle", body, Field.Store.YES));
//			doc.add(new TextField("TextForDescription", body, Field.Store.YES));
			
			
			


			// ====================================================
			// Add the document to the index
			if (idx.getConfig().getOpenMode() == OpenMode.CREATE) {
				idx.addDocument(doc);
			} else {
				idx.updateDocument(new Term("Id", id.toString()), doc);
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

	public void indexSearch(Analyzer analyzer, Similarity similarity, String runTag) {
		System.out.println("Quering and results...");

		//The index reader
		IndexReader reader = null;
		Writer writer = null;
//		int queryId = 0;

		try {
//			String submissionName = "baseline3_w0.9.txt";
			// ficheiro para escrever os resultados para a avaliação
			String submissionName = "src/evaluation/results_java/"+runTag + ".txt";
//			String submissionName = "results.txt";
			// numero de tweets a guardar
			int numberOfTweets = 100;
			// Para fazer debug, fas print do titulo do topic e dos primeiros numberResults resultados
			boolean debug = false;
			int numberResults = 2;
			
			
			writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(submissionName), "utf-8"));
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
			
//			String[] fields = {"Day", "TextForTitle", "TextForDescription" };
			String[] fields = {"Day", "Text", "Text" };
			BooleanClause.Occur[] flags = {BooleanClause.Occur.MUST, BooleanClause.Occur.MUST, BooleanClause.Occur.MUST};

			// Para percorrer os dias
			String [] days = new String [10];
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
			
			Query query = null;
			

			try {
				JSONParser parser = new JSONParser();
				
				JSONArray topics = (JSONArray) parser.parse(new FileReader(topicPath));
				
				String topicTitle = "";
				String topicId = "";
				
				int i = 0;
				//Percorrer os dias
				for (String day : days){
					i++;
					for (Object topicObject : topics){
						
						// get the topic and its atributes
						JSONObject topic = (JSONObject) topicObject;
						topicId = (String) topic.get("topid");
						queryL[0] = day;
						topicTitle = (String)  topic.get("title");
						queryL[1] = topicTitle;
						queryL[2] = (String)  topic.get("description");
						
						
						try {
							query = MultiFieldQueryParser.parse(queryL, fields, flags, analyzer);
						} catch (org.apache.lucene.queryparser.classic.ParseException e) {
							// some topics have bad char
							queryL[1] = queryL[1].replace("\"", "");
							queryL[2] = queryL[2].replace("\"", "");
							
							queryL[1] = queryL[1].replace("/", "");
							queryL[2] = queryL[2].replace("/", "");
							
							try {
								query = MultiFieldQueryParser.parse(queryL, fields, flags, analyzer);
							} catch (org.apache.lucene.queryparser.classic.ParseException e2) {
								System.out.println("Error parsing query string.");
								System.out.println(topicId);
								System.out.println(queryL[1]);
								System.out.println(queryL[2]);
								e2.printStackTrace();
								System.exit(0);
							}
						}
						
						
//						//look on the index, returning the top numberOfTweets answers
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
							// data para o ficheiro
							String dateToWrite = dd[5] + "08" + dd[2];
							float score = hits[j].score;
							
							
							//TODO: Add userScore
							
							if (debug && j<numberResults){
								String text = doc.get("Text");
								String hashtags = doc.get("Hashtags");
								System.out.println(date);
								System.out.println(text);
								System.out.println(hashtags);
							}
							//escrever para o ficheiro
							writeToFile(writer, dateToWrite, topicId, tweetId, (j+1), score, runTag);

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
			try {
				writer.close();
				} catch (Exception ex) {
					ex.printStackTrace();
					}
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
			writer.write(date + "\t" + topic_id + "\t" + "0" + "\t" + tweet_id + "\t"+ rank +"\t" + score + "\t" + ("#" + runTag) + "\n");
			writer.flush();

		}catch (IOException e1) {
			return false;
		}

		return true;
	}

}
