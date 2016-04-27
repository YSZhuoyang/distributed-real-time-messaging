package activitystreamer.client;

public class ClientInfo
{
	private String username;
	private String secret;
	
	public ClientInfo(String u, String s)
	{
		username = u;
		secret = s;
	}

	public void setUsername(String u)
	{
		username = u;
	}
	
	public void setSecret(String s)
	{
		secret = s;
	}
	
	public String getUsername()
	{
		return username;
	}
	
	public String getSecret()
	{
		return secret;
	}
}
