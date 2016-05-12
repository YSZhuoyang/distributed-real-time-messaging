package activitystreamer.server.Broadcaster;

import activitystreamer.util.Connection;


public class BroadcasterInfo
{
	private String id = null;
	private String remoteHostname = null;
	private Connection connection = null;
	private int remotePort = 3780;
	private int serverLoad = 0;
	private boolean connected;

	public String getId()
	{
		return id;
	}
	
	public void setId(String id)
	{
		this.id = id;
	}
	
	public void setConnected(boolean connected)
	{
		this.connected = connected;
	}
	
	public boolean connected()
	{
		return connected;
	}
	
	public String getRemoteHostname()
	{
		return remoteHostname;
	}
	
	public void setRemoteHostname(String remoteHostname)
	{
		this.remoteHostname = remoteHostname;
	}
	
	public Connection getConnection()
	{
		return connection;
	}
	
	public void setConnection(Connection con)
	{
		connection = con;
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
