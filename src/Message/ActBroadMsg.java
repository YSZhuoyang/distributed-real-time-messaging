package Message;


public class ActBroadMsg extends JsonMessage
{
	//private String activity = "";
	//private Activity activity = null;
	public ActBroadMsg()
	{
		setCommand(JsonMessage.ACTIVITY_BROADCAST);
	}

	public void setActivity(String a)
	{
		//activity = a;
	}
}
