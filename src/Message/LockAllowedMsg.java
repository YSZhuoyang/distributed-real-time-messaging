package Message;

public class LockAllowedMsg extends JsonMessage
{
	private String server = "";
	private String secret = "";
	private String username = "";

	public LockAllowedMsg()
	{
		setCommand("LOCK_ALLOWED");
	}

	public void setUsername(String u)
	{
		username = u;
	}
	
	public void setServer(String s)
	{
		server = s;
	}
	
	public void setSecret(String s)
	{
		secret = s;
	}
}
