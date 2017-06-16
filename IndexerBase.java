import java.io.BufferedWriter;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.NavigableMap;
import java.util.Set;
import java.util.TreeMap;

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
 * Indexer base, compara o titulo e a descrição com o texto do tweet
 */
public class IndexerBase extends IndexerAbstract {



	public IndexerBase(boolean useReduced,boolean test){
		super(useReduced,test);
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
			 */
			//Para correr sem hashtags sim
			String[] queryL = new String[3];
			String[] fields = {"Day", "Text", "Text" };
			BooleanClause.Occur[] flags = {BooleanClause.Occur.MUST, BooleanClause.Occur.MUST, BooleanClause.Occur.SHOULD};
			
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
						
						queryL[0] = day;
						queryL[1] = topicTitle;
						queryL[2] = description;
						
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
						
						TreeMap<Double, Integer> orderedSet = new TreeMap<Double, Integer>();
						
						// há scores repetidos, é necessário guardar-los
						Map <Double,List<Integer>> valuesToOrder = new HashMap<Double,List<Integer>>();
					
						
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
								System.out.println("\nScore: " + score);
								String userId = doc.get("UserId");
								double scoreFromUser = ranksForUsers.getUserScore(userId);
//								score = (0.8*(score/100)) + 0.2*scoreFromUser;
								score = (1.0*(score)) + 0.0*scoreFromUser;
								System.out.println("Score from user: " + scoreFromUser);
								System.out.println("New score: " + score);
								

								if(!orderedSet.containsKey(score))
									orderedSet.put(score, j);
								else
									if(valuesToOrder.containsKey(score))
										valuesToOrder.get(score).add(j);
									else{
										List<Integer> l = new LinkedList<Integer>();
										l.add(j);
										valuesToOrder.put(score, l);
										orderedSet.put(score, j);
									}
							}
							
							if (debug && j<numberResults){
								String text = doc.get("Text");
								String hashtags = doc.get("Hashtags");
								System.out.println(date);
								System.out.println(text);
								System.out.println("#" + hashtags);
							}
							//escrever para o ficheiro
							if (!userScore) writeToFile(writer, dateToWrite, topicId, tweetId, (j+1), score, runTag);

						}
						
						if (userScore){
							
						
							
							Set<Entry<Double, Integer>> set = orderedSet.entrySet();
//							
//							NavigableMap <Double, Integer> des = orderedSet.descendingMap();
//							
//							Iterator it = des
//									
//
//									NavigableMap <Long, String> nmap = treeMap.descendingMap();
//
//							Set<Long, String> set = nmap.entrySet();
//
//							Iterator<Long, String> iterator = set.iterator();
							
							Iterator<Entry<Double, Integer>>  it = set.iterator();
							int x = 1;
							while(it.hasNext()){
								Entry<Double, Integer> run = it.next();
								int posDoc = run.getValue();
								double scoreDoc = run.getKey();
								if(valuesToOrder.containsKey(scoreDoc))
									for(int pos: valuesToOrder.get(scoreDoc)){
										Document doc = searcher.doc(hits[pos].doc);
										String tweetId = doc.get("Id");
										String date = doc.get("Date");
										String[] dd = date.split(" ");
										// data para o ficheiro
										String dateToWrite = dd[5] + "08" + dd[2];
										
										writeToFile(writer, dateToWrite, topicId, tweetId, x, scoreDoc, runTag);
										x++;
									}
								else{
									Document doc = searcher.doc(hits[posDoc].doc);
									String tweetId = doc.get("Id");
									String date = doc.get("Date");
									String[] dd = date.split(" ");
									// data para o ficheiro
									String dateToWrite = dd[5] + "08" + dd[2];
									
									writeToFile(writer, dateToWrite, topicId, tweetId, x, scoreDoc, runTag);
									x++;
								}
//								Document doc = searcher.doc(hits[posDoc].doc);
//								String tweetId = doc.get("Id");
//								String date = doc.get("Date");
//								String[] dd = date.split(" ");
//								// data para o ficheiro
//								String dateToWrite = dd[5] + "08" + dd[2];
//								
//								writeToFile(writer, dateToWrite, topicId, tweetId, x, scoreDoc, runTag);
//								x++;
							}
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



}
