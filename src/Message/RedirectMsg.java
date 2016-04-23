package Message;

public class RedirectMsg extends JsonMessage
{
	private String host = "";
	private String port = "";

	public RedirectMsg()
	{
		command = "REDIRECT";
	}

	public void setHost(String h)
	{
		host = h;
	}
	
	public void setPort(String p)
	{
		port = p;
	}
}
