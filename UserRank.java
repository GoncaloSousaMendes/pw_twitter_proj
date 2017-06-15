import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.TimeUnit;

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
/**
 * @author Moncada
 *
 */
public class UserRank {
	
	private long maxFollowers;
	private Map <String, User> users;
	
	public UserRank (){
		maxFollowers = 0;
		users = new HashMap<String, User>();
	}
	
	public long getmaxFollowers(){
		return maxFollowers;
	}
	
	public void saveUser(JSONObject user){
		String Id = (String) user.get("id_str");
		if(users.containsKey(Id))
			users.get(Id).incrementNumberOfTweets();
		else{
			long followers = (long) user.get("followers_count");
			long numberTweets = (long) user.get("statuses_count");
			String dateOfCreation = (String) user.get("created_at");
			
			long daysOnT = this.computeDaysOnTwitter(dateOfCreation); 
			
			
			User u = new UserClass(Id, followers, daysOnT, numberTweets);
			users.put(Id, u);
			
			maxFollowers = maxFollowers < followers ? followers : maxFollowers;
			
		}
	}
	
	private long computeDaysOnTwitter(String date){

		
		String [] dateSep = date.split(" ");

		String month = this.getMonth(dateSep[1]);

		DateFormat dateFormat = new SimpleDateFormat("yyyy/MM/dd");
		Date dateToday = new Date();
//		System.out.println(dateFormat.format(dateToday)); //2016/11/16
		
		String dateT = dateSep[5] + "/" +  month + "/" + dateSep[2];
//		System.out.println(dateT);
		Date dateTwitter = null;
		try {
			dateTwitter = dateFormat.parse(dateT);
		} catch (ParseException e) {
			e.printStackTrace();
		}

		long differenceInDays = TimeUnit.MILLISECONDS.toDays(dateToday.getTime()-dateTwitter.getTime());
//		System.out.println(differenceInDays);
		
		return differenceInDays;
	}
	
	private String getMonth(String month){
		if(month.equals("Jan"))
			return "01";
		
		else if(month.equals("Feb"))
			return "02";
		
		else if(month.equals("Mar"))
			return "03";
		
		else if(month.equals("Apr"))
			return "04";
		
		else if(month.equals("May"))
			return "05";
		
		else if(month.equals("Jun"))
			return "06";
		
		else if(month.equals("Jul"))
			return "07";
		
		else if(month.equals("Aug"))
			return "08";
		
		else if (month.equals("Sep")) 
			return "09";
		
		else if(month.equals("Oct"))
			return "10";
		
		else if(month.equals("Nov"))
			return "11";
		
		else if(month.equals("Dec"))
			return "12";
		
		else{
			System.out.println("Error on month: " + month);
			System.exit(0);
		}
		
		
		return "";
	}
	
	public void scoreUsers(){
		Collection <User> usersSet = users.values();
		Iterator<User> it = usersSet.iterator();
		
		while(it.hasNext()){
			User u = it.next();
			long nTweets = u.getNumberOfTweets();
			long nDays = u.getDaysOnTwitter();
			long nFol = u.getNumberOfFollowers();
			double score =  (((double)nTweets/(double)nDays)) * (((double)nFol/(double)maxFollowers));
			
			u.setScore(score);
//			System.out.println("");
//			System.out.println("Number tweets: " + nTweets);
//			System.out.println("Number days: " + nDays);
//			System.out.println("Number followers: " + nFol);
//			System.out.println(u.getNumberOfTweetsOnTREC());
//			System.out.println(score);
		}
		
	}
	
	public int getNumberOfUsers(){
		return users.size();
	}
	
	public double getUserScore(String id){
		return users.containsKey(id) ? users.get(id).getScore() : 0.0;

	}

}
