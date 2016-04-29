package Message;

public class ServerAnnounceMsg extends JsonMessage
{
	private String id = "";
	private String hostname = "";
	private int load = 0;
	private int port = 0;

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
		this.hostname = host;
	}
	
	public void setPort(int port)
	{
		this.port = port;
	}
	
	public void setLoad(int load)
	{
		this.load = load;
	}
}
