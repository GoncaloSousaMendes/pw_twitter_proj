import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;

import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.TextField;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

public class TweetTester {
	
	private static String tweetsPath = "src/tweets/rts2016-qrels-tweets2016.jsonl";
	
	private static String qrels = "src/evaluation/rts2016-qrels.txt";
	

	static Map <String, JSONObject> tweetsMap;
	static Map <String, Integer> topicMap;
	
	//TreeMap<String, Double> sorted_map = new TreeMap<String, Double>(bvc);
	
	
	/* TODO
	 * Guardar todos os tweets num Map
	 * Ver se as reply são de algum tweet que temos
	 * (comparar os textos)
	 * Percorrer por utilizadores e estudar...
	 * Fazer o social graph
	 * 
	 */
	
	public static void main(String[] args) {
		

		
		try{    
		    Process p = Runtime.getRuntime().exec(" cmd /c  run_eval.bat run_test.txt");
		    p.waitFor();

		}catch( IOException ex ){
		    //Validate the case the file can't be accesed (not enought permissions)

		}catch( InterruptedException ex ){
		    //Validate the case the process is being stopped by some external situation     

		}
		
//		System.out.println("Got here!");
		
//		tweetsMap = new HashMap<String, JSONObject>();
//		topicMap = new HashMap<String, Integer>();
//		
//		JSONParser parser = new JSONParser();
//		try (BufferedReader br = new BufferedReader(new FileReader(tweetsPath))) {
//
//			String line = br.readLine(); 
//			Object obj;
//			int i = 0;
//			while(line != null){
//				obj = parser.parse(line);
//				JSONObject tweet = (JSONObject) obj;
//				String Id = (String) tweet.get("id_str");
//				if(!tweetsMap.containsKey(Id))
//					tweetsMap.put(Id, tweet);
//				else {
////					System.out.println("In the map: " + tweetsMap.get(Id).get("created_at"));
////					System.out.println(tweetsMap.get(Id).get("text"));
////					
////					System.out.println("New: " + tweet.get("created_at"));
////					System.out.println(tweet.get("text"));
////					System.out.println("\n");
//					i++;
//				}
//				line = br.readLine();
//			}
//			
//			System.out.println("Number of repetitions: " + i);
//
//			
//		} catch (IOException e) {
//
//			e.printStackTrace();
//		} catch (org.json.simple.parser.ParseException e) {
//
//			e.printStackTrace();
//		}
//		
		
		
		
		
		//Read the qrels
//		try (BufferedReader br = new BufferedReader(new FileReader(qrels))) {
//			
//			String line = br.readLine(); 
//			while(line != null){
//				String [] data = line.split(" ");
//				String topic_id = data[0];
//				String tweet_id = data[2];
//				
//				
//				if (tweetsMap.containsKey(tweet_id))
//					if(topicMap.containsKey(topic_id) )
//						topicMap.replace(topic_id, topicMap.get(topic_id), (topicMap.get(topic_id)+1));
//					else if (!topicMap.containsKey(topic_id))
//						topicMap.put(topic_id, 1);
//
//			}
//			
//			TreeMap <String, Integer> topicsOrdered = new TreeMap<String, Integer>();
//			
//			topicsOrdered.putAll(topicMap);
//			
//			System.out.println(topicsOrdered);
//			
//		}catch (IOException e) {
//
//			e.printStackTrace();
//		} 
//		
//		
	}
	

	
	

}
