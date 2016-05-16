package activitystreamer.server.Broadcaster;


public class BroadcasterInfo
{
	private String id = null;
	private String remoteHostname = null;
	private int remotePort = 3780;
	private int serverLoad = 0;

	public String getId()
	{
		return id;
	}
	
	public void setId(String id)
	{
		this.id = id;
	}
	
	public String getRemoteHostname()
	{
		return remoteHostname;
	}
	
	public void setRemoteHostname(String remoteHostname)
	{
		this.remoteHostname = remoteHostname;
	}
	
	public int getRemotePort()
	{
		return remotePort;
	}
	
	public void setRemotePort(int remotePort)
	{
		this.remotePort = remotePort;
	}
	
	public int getServerLoad()
	{
		return serverLoad;
	}
	
	public void setServerLoad(int serverLoad)
	{
		this.serverLoad = serverLoad;
	}
}
