package Message;

public class JsonMessage
{
	protected String command = "";
	
	public final static String REGISTER_FAILED = "REGISTER_FAILED";
	public final static String REDIRECT = "REDIRECT";
	public final static String LOGIN_FAILED = "LOGIN_FAILED";
	public final static String LOGIN = "LOGIN";
	public final static String REGISTER = "REGISTER";
	
	public JsonMessage()
	{
		
	}
	
	protected void setCommand(String c)
	{
		command = c;
	}
}
