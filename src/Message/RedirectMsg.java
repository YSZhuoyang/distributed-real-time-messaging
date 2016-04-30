package Message;

public class RedirectMsg extends JsonMessage
{
	private String hostname = "";
	private int port = 0;

	public RedirectMsg()
	{
		setCommand(JsonMessage.REDIRECT);
	}

	public void setHost(String h)
	{
		hostname = h;
	}
	
	public void setPort(int p)
	{
		port = p;
	}
}
