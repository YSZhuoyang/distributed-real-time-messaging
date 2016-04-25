package Message;

public class LockDeniedMsg extends JsonMessage
{
	private String username = "";
	private String secret = "";

	public LockDeniedMsg()
	{
		setCommand(JsonMessage.LOCK_DENIED);
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
