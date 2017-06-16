import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.index.Term;
import org.apache.lucene.index.IndexWriterConfig.OpenMode;
import org.apache.lucene.queryparser.classic.MultiFieldQueryParser;
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

/**
 * 
 */

/**
 * @author Moncada
 *
 */
public abstract class IndexerAbstract implements Indexer {
	
	
//	protected String tweetsPath = "src/tweets/rts2016-qrels-tweets2016.jsonl";
	protected String tweetsPath = "src/tweets/rts2016-qrels-sim-tweets2016.jsonl";
	
	
	
	protected String indexPath = "src/index";
//	protected String topicPath = "src/profiles/pw_top_10_topics.json";
	protected String topicPath = "src/profiles/pw_top_stable_topics.json";
	
	
	protected UserRank ranksForUsers;
	
	//save the tweets, to find repetitions
	protected static Map <String, JSONObject> tweetsMap;

	private boolean create = true;

	private IndexWriter idx;
	
	public IndexerAbstract(){
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
			
			String userId = (String)  ( (JSONObject) tweet.get("user")).get("id_str");
			doc.add(new TextField("UserId", userId, Field.Store.YES));		
					
			
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
					tags += hash.get("text") + " ";
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

	public void indexSearch(Analyzer analyzer, Similarity similarity, String runTag, boolean userScore) {
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
			boolean debug = true;
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
			//Para correr sem hashtags sim
//			String[] queryL = new String[3];
//			String[] fields = {"Day", "Text", "Text" };
//			BooleanClause.Occur[] flags = {BooleanClause.Occur.MUST, BooleanClause.Occur.MUST, BooleanClause.Occur.SHOULD};
			
			// Para correr com hashtags sim
			String[] queryL = new String[4];
			String[] fields = {"Day", "Text", "Text", "Hashtags" };
			BooleanClause.Occur[] flags = {BooleanClause.Occur.MUST, BooleanClause.Occur.MUST, BooleanClause.Occur.SHOULD, BooleanClause.Occur.SHOULD};

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
				
//				int i = 0;
				//Percorrer os dias
				for (String day : days){
//					i++;
					for (Object topicObject : topics){
						
						// get the topic and its atributes
						JSONObject topic = (JSONObject) topicObject;
						topicId = (String) topic.get("topid");
						topicTitle = (String)  topic.get("title");
						String description = (String)  topic.get("description");
						
						queryL[0] = day;
						queryL[1] = topicTitle;
						queryL[2] = description;
						//o titulo vai ver a semelhanca com os hastags
						queryL[3] = topicTitle;
						
						try {
							query = MultiFieldQueryParser.parse(queryL, fields, flags, analyzer);
						} catch (org.apache.lucene.queryparser.classic.ParseException e) {
							// some topics have bad char
							for (int i=0; i< queryL.length; i++){
								queryL[i] = queryL[i].replace("\"", "");
								queryL[i] = queryL[i].replace("/", "");
							}

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
							System.out.println(description);
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
							double score = hits[j].score;

							if (userScore){
//								System.out.println("\nScore:" + score);
								String userId = doc.get("UserId");
								double scoreFromUser = ranksForUsers.getUserScore(userId);
								score = 0.8*score + 0.2*scoreFromUser;
//								System.out.println("Score from user: " + scoreFromUser);
//								System.out.println("New score:" + score);
							}
							
							if (debug && j<numberResults){
								String text = doc.get("Text");
								String hashtags = doc.get("Hashtags");
								System.out.println(date);
								System.out.println(text);
								System.out.println("#" + hashtags);
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
	
	public void close() {
		try {
			idx.close();
		} catch (IOException e) {
			System.out.println("Error closing the index.");
		}
	}

	protected boolean writeToFile(Writer writer, String date, String topic_id, String tweet_id, int rank, double score, String runTag){
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
