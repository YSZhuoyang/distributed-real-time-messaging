package activitystreamer.server;

import java.util.ArrayList;

public class LockInfo
{
	private String username;
	private String secret;
	private ArrayList<String> allowedServerList;
	private Connection connection;
	
	public LockInfo(String u, String s)
	{
		username = u;
		secret = s;
		allowedServerList = new ArrayList<>();
	}

	public void setUsername(String u)
	{
		username = u;
	}
	
	public void setSecret(String s)
	{
		secret = s;
	}
	
	public void setConnection(Connection con)
	{
		connection = con;
	}
	
	public void addAllowedServer(String id)
	{
		allowedServerList.add(id);
	}
	
	public String getUsername()
	{
		return username;
	}
	
	public String getSecret()
	{
		return secret;
	}
	
	public Connection getConnection()
	{
		return connection;
	}
	
	public boolean lockAllowedMsgReceivedFromAllServers(ArrayList<ServerInfo> serverInfoList)
	{
		for (ServerInfo serverInfo : serverInfoList)
		{
			if (!allowedServerList.contains(serverInfo.getId()))
			{
				return false;
			}
		}
		
		return true;
	}
}
