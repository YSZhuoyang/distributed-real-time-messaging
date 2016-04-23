package Message;

public class JsonMessage
{
	private String command = "LOGIN";
	private String username = "";
	private String secret = "";
	
	public JsonMessage()
	{
		
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
