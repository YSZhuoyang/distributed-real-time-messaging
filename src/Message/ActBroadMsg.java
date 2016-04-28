package Message;

import com.ibm.common.activitystreams.Activity;

public class ActBroadMsg extends JsonMessage
{
	//private String activity = "";
	private Activity userActivity = null;
	public ActBroadMsg()
	{
		setCommand(JsonMessage.ACTIVITY_BROADCAST);
	}

	public void setUserActivity(Activity a)
	{
		userActivity = a;
	}
}
