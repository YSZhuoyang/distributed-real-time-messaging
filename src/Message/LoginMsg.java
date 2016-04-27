package Message;

public class LoginMsg extends JsonMessage
{
	private String username = "";
	private String secret = "";

	public LoginMsg()
	{
		setCommand(JsonMessage.LOGIN);
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
