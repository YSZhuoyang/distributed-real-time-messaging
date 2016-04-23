package Message;

public class RegisterMsg extends JsonMessage
{
	private String username = "";
	private String secret = "";

	public RegisterMsg()
	{
		command = "REGISTER";
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
