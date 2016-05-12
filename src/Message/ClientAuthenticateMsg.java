package Message;

public class ClientAuthenticateMsg extends JsonMessage
{
	private String username = "";
	private String secret = "";
	private String id = "";

	public ClientAuthenticateMsg()
	{
		setCommand(JsonMessage.CLIENT_AUTHENTICATE);
	}
	
	public void setId(String i)
	{
		id = i;
	}

	public void setUsername(String n)
	{
		username = n;
	}
	
	public void setSecret(String s)
	{
		secret = s;
	}
}
