package Message;

public class RedirectMsg extends JsonMessage
{
	private String hostname = "";
	private String id = "";
	private int port = 0;

	public RedirectMsg()
	{
		setCommand(JsonMessage.REDIRECT);
	}

	public void setHost(String h)
	{
		hostname = h;
	}
	
	public void setId(String id)
	{
		this.id = id;
	}
	
	public void setPort(int p)
	{
		port = p;
	}
}
