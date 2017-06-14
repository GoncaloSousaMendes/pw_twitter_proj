import java.util.Date;

/**
 * @author Moncada
 *
 */
public interface User {
	
	public String getId();

	long getNumberOfFollowers();

	long getDaysOnTwitter();
	
	double getScore();

	void setScore(double score);

	long getNumberOfTweets();
	
	void incrementNumberOfTweets();
	
	public int getNumberOfTweetsOnTREC();

}
