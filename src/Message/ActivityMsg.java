package Message;

import com.ibm.common.activitystreams.Activity;

public class ActivityMsg extends RedirectMsg
{
	private Activity userActivity = null;
	private String username = "";
	private String secret = "";

	public ActivityMsg()
	{
		setCommand(JsonMessage.ACTIVITY_MESSAGE);
	}
	
	public void setUserActivity(Activity a)
	{
		userActivity = a;
	}
	
	public void setUsername(String u)
	{
		username = u;
	}
	
	public void setSecret(String s)
	{
		secret = s;
	}
}
