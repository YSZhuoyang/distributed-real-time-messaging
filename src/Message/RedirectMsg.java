package Message;

public class RedirectMsg extends JsonMessage
{
	private String host = "";
	private int port = 0;

	public RedirectMsg()
	{
		setCommand(JsonMessage.REDIRECT);
	}

	public void setHost(String h)
	{
		host = h;
	}
	
	public void setPort(int p)
	{
		port = p;
	}
}
