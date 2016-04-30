package Message;

public class AnonymousLoginMsg extends JsonMessage
{
	private String username = "";
	private String secret = "";

	public AnonymousLoginMsg()
	{
		setCommand(JsonMessage.LOGIN);
		setUsername(JsonMessage.ANONYMOUS_USERNAME);
	}

	private void setUsername(String n)
	{
		username = n;
	}
}
