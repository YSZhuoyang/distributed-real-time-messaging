package Message;

public class AnonymusLoginMsg extends JsonMessage
{
	private String username = "";
	private String secret = "";

	public AnonymusLoginMsg()
	{
		setCommand(JsonMessage.LOGIN);
		setUsername(JsonMessage.ANONYMUS_USERNAME);
	}

	private void setUsername(String n)
	{
		username = n;
	}
}
