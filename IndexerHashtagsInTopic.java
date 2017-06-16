import java.io.BufferedWriter;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.file.Paths;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.queryparser.classic.MultiFieldQueryParser;
import org.apache.lucene.search.BooleanClause;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.search.similarities.Similarity;
import org.apache.lucene.store.FSDirectory;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

/*
 * Indexer que associa as hashtags do top n tweets ao topico,
 * este começa com as hashtags que são retiradas do titulo do topico
 */

public class IndexerHashtagsInTopic extends IndexerAbstract {

	// save the hashtags for topic
	// key: topic_id; value: hashtags
	protected Map <String, String> hashTagForTopic;
	
	public IndexerHashtagsInTopic(boolean useReduced,boolean test) {
		super(useReduced,test);
		hashTagForTopic = new HashMap<String, String>();
	}
	
	@Override
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
			 * Na posição 3 -> hashtags retiradas do topic
			 */
			//Para correr sem hashtags sim
			// Para correr com hashtags sim
			String[] queryL = new String[4];
			// correspondem:   dia    titulo  descrição  hashstags
			String[] fields = {"Day", "Text",  "Text",    "Text" };
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
				
				//adicionar as hashtags ao topic -> hashTagForTopic <String, String>
				for (Object topicObject : topics){
					JSONObject topic = (JSONObject) topicObject;
					topicId = (String) topic.get("topid");
					topicTitle = (String)  topic.get("title");
					
					if(!hashTagForTopic.containsKey(topicId))
						hashTagForTopic.put(topicId, topicTitle);
					else
						System.out.println("Topic Id repeated!");	
				}

				//Percorrer os dias
				for (String day : days){
					if (debug){
						System.out.println("\n-------------------------------------------------------------------------------------------");
						System.out.println("------------------------------------NEW DAY------------------------------------------------");
						System.out.println("-------------------------------------------------------------------------------------------\n");
					}

					for (Object topicObject : topics){
						
						// get the topic and its atributes
						JSONObject topic = (JSONObject) topicObject;
						topicId = (String) topic.get("topid");
						topicTitle = (String)  topic.get("title");
						String description = (String)  topic.get("description");
						String tags = hashTagForTopic.get(topicId);
						
						queryL[0] = day;
						queryL[1] = topicTitle;
						queryL[2] = description;
						queryL[3] = tags;
						
						// some topics have bad char
						for (int i=0; i< queryL.length; i++){
							queryL[i] = queryL[i].replace("\"", "");
							queryL[i] = queryL[i].replace("/", "");
						}
						
						try {
							query = MultiFieldQueryParser.parse(queryL, fields, flags, analyzer);
						} catch (org.apache.lucene.queryparser.classic.ParseException e) {
							System.out.println("Error parsing query string.");
							System.out.println(topicId);
							System.out.println(queryL[1]);
							System.out.println(queryL[2]);
							e.printStackTrace();
							System.exit(0);


//							try {
//								query = MultiFieldQueryParser.parse(queryL, fields, flags, analyzer);
//							} catch (org.apache.lucene.queryparser.classic.ParseException e2) {
//								System.out.println("Error parsing query string.");
//								System.out.println(topicId);
//								System.out.println(queryL[1]);
//								System.out.println(queryL[2]);
//								e2.printStackTrace();
//								System.exit(0);
//							}
						}
						
						
//						//look on the index, returning the top numberOfTweets answers
						TopDocs results = searcher.search(query, numberOfTweets);
						ScoreDoc[] hits = results.scoreDocs;
						int numTotalHits = results.totalHits;
						
						if (debug){
							System.out.println("-------------------------------------------------------------------------------------------");
							System.out.println(topicTitle + " " + topicId);
							System.out.println(description);
							System.out.println(tags);
							System.out.println(numTotalHits + " total matching documents");
						}

						String newTags ="";
						Map <String,String> tagsInd = new HashMap<String,String>();
						
						String[] tags_s = tags.split(" ");
						for (String s: tags_s)
							tagsInd.put(s, s);
						
						//iterate through the answers 
						for (int j = 0; j < numberOfTweets && j < numTotalHits; j++) {

							Document doc = searcher.doc(hits[j].doc);
							String tweetId = doc.get("Id");
							String date = doc.get("Date");
							String[] dd = date.split(" ");
							String hashtags = doc.get("Hashtags");
							// data para o ficheiro
							String dateToWrite = dd[5] + "08" + dd[2];
							double score = hits[j].score;
							
							// adicionar as hashtags dos primeiros 5 tweets
							if(j<5){
								
								tags_s = hashtags.split(" ");
								for (String s: tags_s)
									tagsInd.put(s, s);
							}
							
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
								System.out.println(date);
								System.out.println(text);
								System.out.println("#" + hashtags);
							}
							//escrever para o ficheiro
							writeToFile(writer, dateToWrite, topicId, tweetId, (j+1), score, runTag);

						}
						
						Iterator<String> it = tagsInd.keySet().iterator();
						while(it.hasNext())
							newTags += it.next() + " ";

						if(hashTagForTopic.containsKey(topicId))
							hashTagForTopic.put(topicId, newTags);
						
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


}
