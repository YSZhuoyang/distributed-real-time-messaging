package Message;

public class ActivityMsg extends RedirectMsg
{
	private String activity = "";
	private String username = "";
	private String secret = "";

	public ActivityMsg()
	{
		command = "ACTIVITY_MESSAGE";
	}
	
	public void setActivity(String a)
	{
		activity = a;
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
