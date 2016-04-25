package Message;

public class ServerAnnounceMsg extends JsonMessage
{
	private String id = "";
	private String load = "";
	private String host = "";
	private String port = "";

	public ServerAnnounceMsg()
	{
		setCommand(JsonMessage.SERVER_ANNOUNCE);
	}
	
	public void setId(String id)
	{
		this.id = id;
	}
	
	public void setHostname(String host)
	{
		this.host = host;
	}
	
	public void setPort(String port)
	{
		this.port = port;
	}
	
	public void setLoad(String load)
	{
		this.load = load;
	}
}
