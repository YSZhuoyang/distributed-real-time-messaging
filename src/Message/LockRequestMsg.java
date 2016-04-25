package Message;

public class LockRequestMsg extends JsonMessage
{
	private String username = "";
	private String secret = "";

	public LockRequestMsg()
	{
		setCommand("LOCK_REQUEST");
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
