package Message;



public class ActivityMsg extends RedirectMsg
{
	//private Activity activity = null;
	private String username = "";
	private String secret = "";

	public ActivityMsg()
	{
		setCommand(JsonMessage.ACTIVITY_MESSAGE);
	}
	
	public void setActivity(String a)
	{
		
		//Activity thisActivity = activity().object(a)
		//		.get();
		//activity = thisActivity;
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
