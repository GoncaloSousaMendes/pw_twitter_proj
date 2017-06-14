import java.util.Date;

/**
 * 
 */

/**
 * @author Moncada
 *
 */
public class UserClass implements User{
	
	private String Id;
	private long numberOfFollowers;
	private long numberOfTweets;
	private double score;
	private long daysOnTwitter;
	private int numberOfTweetsInTREC;
	

	public UserClass(String id, long followers, long days, long nTweets){
		this.Id = id;
		this.numberOfFollowers = followers;
		this.daysOnTwitter = days;
		this.numberOfTweets = nTweets;
		numberOfTweetsInTREC = 1;
	}
	
	public void incrementNumberOfTweets(){
		numberOfTweetsInTREC++;
	}
	
	public int getNumberOfTweetsOnTREC(){
		return numberOfTweetsInTREC;
	}

	public String getId() {
		return Id;
	}


	public long getNumberOfFollowers() {
		return numberOfFollowers;
	}

	public long getDaysOnTwitter(){
		return daysOnTwitter;
	}
	
	public double getScore() {
		return score;
	}

	public void setScore(double score) {
		this.score = score;
	}

	public long getNumberOfTweets() {
		return numberOfTweets;
	}
}
