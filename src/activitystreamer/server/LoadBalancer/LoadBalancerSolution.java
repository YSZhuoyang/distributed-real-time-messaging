package activitystreamer.server.LoadBalancer;

import java.io.IOException;
import java.net.Socket;
import java.util.ArrayList;
import java.util.HashMap;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonSyntaxException;

import Message.AuthFailMsg;
import Message.InvalidMsg;
import Message.JsonMessage;
import Message.LoginFailedMsg;
import Message.LoginSuccMsg;
import Message.RedirectMsg;
import Message.RegistSuccMsg;
import Message.RegisterFailedMsg;
import activitystreamer.server.Broadcaster.BroadcasterInfo;
import activitystreamer.util.Connection;
import activitystreamer.util.Control;
import activitystreamer.util.Settings;

public class LoadBalancerSolution extends Control
{
	private static final int CLIENT_CONNECTION_UPPER_LIMIT = 50;
	protected static final Logger log = LogManager.getLogger();
	
	private ArrayList<Connection> serverConnectionList = new ArrayList<>();

	// All servers except itself
	private ArrayList<BroadcasterInfo> serverInfoList = new ArrayList<>();
	private HashMap<String, String> clientInfoList = new HashMap<>();
	private LoadBalancerListener listener;
	
	private static LoadBalancerSolution loadBalancerSolution;
	
	public static synchronized LoadBalancerSolution getInstance()
	{
		if (loadBalancerSolution == null)
		{
			loadBalancerSolution = new LoadBalancerSolution();
		}

		return loadBalancerSolution;
	}
	
	private LoadBalancerSolution()
	{
		super();
		
		// initialize the connections array
		serverConnectionList = new ArrayList<Connection>();
		
		// generate secrete keys here
		String secret = Settings.nextSecret();
		Settings.setSecret(secret);

		log.info("The secret key of all servers is: " + secret);
		
		// start a listener
		listener = LoadBalancerListener.getInstance();
		
		start();
	}
	
	public LoadBalancerConnection incomingConnection(Socket s) throws IOException
	{
		log.debug("incomming connection: " + Settings.socketAddress(s));
		
		LoadBalancerConnection c = new LoadBalancerConnection(s);

		return c;
	}
	
	public synchronized boolean process(Connection con, String msg)
	{
		log.debug("Server Receieved: " + msg);
		
		JsonObject receivedJsonObj;
		
		try
		{
			receivedJsonObj = new Gson().fromJson(msg, JsonObject.class);
		}
		catch (JsonSyntaxException e)
		{
			log.debug("Server receiving msg failed. Not json format: " + e.getMessage());
			
			return true;
		}
		
		if (!containCommandField(con, receivedJsonObj))
		{
			return true;
		}
		
		String msgType = receivedJsonObj.get("command").getAsString();

		switch (msgType)
		{
			case JsonMessage.LOGIN:
				return processLoginMsg(con, receivedJsonObj);
				
			case JsonMessage.REGISTER:
				return processRegisterMsg(con, receivedJsonObj);
				
			case JsonMessage.AUTHENTICATE:
				return processAuthMsg(con, receivedJsonObj);

			case JsonMessage.INVALID_MESSAGE:
				return processInvalidMsg(receivedJsonObj);
				
			case JsonMessage.SERVER_ANNOUNCE:
				return processServerAnnounceMsg(con, receivedJsonObj);

			default:
				return processInvalidCommand(con, receivedJsonObj);
		}
	}
	
	private boolean processLoginMsg(Connection con, JsonObject receivedJsonObj)
	{
		// Validate login message format
		if (!isUserInfoMsgValid(con, receivedJsonObj))
		{
			return true;
		}
		
		String secret = receivedJsonObj.get("secret").getAsString();
		String username = receivedJsonObj.get("username").getAsString();

		// Secret or username is not correct, login failed
		if (!username.equals(JsonMessage.ANONYMOUS_USERNAME) && !hasClientInfo(username, secret))
		{
			log.info("attempt to login with a wrong secret");

			LoginFailedMsg loginFailedMsg = new LoginFailedMsg();
			loginFailedMsg.setInfo("user attempt to login with a wrong secret");

			String registFailedJsonStr = loginFailedMsg.toJsonString();
			con.writeMsg(registFailedJsonStr);

			return true;
		}

		// Find a server with less client connection
		BroadcasterInfo serverInfo = loadBalance();
		
		if (serverInfo != null)
		{
			log.info("logged in as user " + username);

			LoginSuccMsg loginSuccMsg = new LoginSuccMsg();
			loginSuccMsg.setInfo("Login successful");

			String loginSuccJsonStr = loginSuccMsg.toJsonString();
			con.writeMsg(loginSuccJsonStr);
			
			log.info("Redirected");

			RedirectMsg redirectMsg = new RedirectMsg();
			redirectMsg.setHost(serverInfo.getRemoteHostname());
			redirectMsg.setPort(serverInfo.getRemotePort());
			redirectMsg.setId(serverInfo.getId());

			String redirectMsgJsonStr = redirectMsg.toJsonString();
			con.writeMsg(redirectMsgJsonStr);
		}
		// All servers are too busy
		else
		{
			log.info("server is too busy");
			
			LoginFailedMsg loginFailedMsg = new LoginFailedMsg();
			loginFailedMsg.setInfo("server is too busy");

			String registFailedJsonStr = loginFailedMsg.toJsonString();
			con.writeMsg(registFailedJsonStr);
		}

		return true;
	}
	
	private boolean processRegisterMsg(Connection con, JsonObject receivedJsonObj)
	{
		// Validate register message format
		if (!isUserInfoMsgValid(con, receivedJsonObj))
		{
			return true;
		}
		
		String secret = receivedJsonObj.get("secret").getAsString();
		String username = receivedJsonObj.get("username").getAsString();

		// Check whether username already exists, and username cannot be 'anonymous'
		if (clientInfoList.containsKey(username) || username.equals(JsonMessage.ANONYMOUS_USERNAME))
		{
			log.info("Register failed. Username already exists!");

			RegisterFailedMsg registerFailedMsg = new RegisterFailedMsg();
			registerFailedMsg.setInfo(username + " is already registered in the system");

			String registFailedJsonStr = registerFailedMsg.toJsonString();
			con.writeMsg(registFailedJsonStr);

			return true;
		}
		// Register success
		else
		{
			log.info("Register_Success");

			// Send register success message
			RegistSuccMsg registerSuccMsg = new RegistSuccMsg();
			registerSuccMsg.setInfo("register success for " + username);

			String registSuccJsonStr = registerSuccMsg.toJsonString();
			con.writeMsg(registSuccJsonStr);

			// Add client info
			clientInfoList.put(username, secret);
			
			return true;
		}
	}
	
	private boolean processAuthMsg(Connection con, JsonObject receivedJsonObj)
	{
		// Json message format incorrect
		if (!receivedJsonObj.has("secret"))
		{
			log.info("Auth faield: the supplied secret is incorrect");

			AuthFailMsg authFailedMsg = new AuthFailMsg();
			authFailedMsg.setInfo("the supplied secret is incorrect");
			
			String authFailedJsonStr = authFailedMsg.toJsonString();
			con.writeMsg(authFailedJsonStr);

			return true;
		}
		
		String secret = receivedJsonObj.get("secret").getAsString();

		if (!secret.equals(Settings.getSecret()))
		{
			log.info("Auth faield");

			AuthFailMsg authFailedMsg = new AuthFailMsg();
			authFailedMsg.setInfo("the supplied secret is incorrect: " + secret);

			String authFailedJsonStr = authFailedMsg.toJsonString();
			con.writeMsg(authFailedJsonStr);

			return true;
		}
		// Server already authenticated, send invalid message
		else if (isServerAuthenticated(con))
		{
			return true;
		}
		// Connect with server
		else
		{
			log.info("Auth succeeded");

			serverConnectionList.add(con);

			return false;
		}
	}

	private boolean processServerAnnounceMsg(Connection con, JsonObject receivedJsonObj)
	{
		log.info("Server announce received");
		
		if (!serverConnectionList.contains(con))
		{
			// Send invalid message
			InvalidMsg invalidMsg = new InvalidMsg();
			invalidMsg.setInfo(JsonMessage.UNAUTHENTICATED_SERVER);
			con.writeMsg(invalidMsg.toJsonString());
			
			return true;
		}

		String id = receivedJsonObj.get("id").getAsString();
		BroadcasterInfo serverInfo = findServer(id);

		// This is a new server
		if (serverInfo == null)
		{
			serverInfo = new BroadcasterInfo();
			serverInfo.setId(id);
			serverInfo.setServerLoad(receivedJsonObj.get("load").getAsInt());
			serverInfo.setRemoteHostname(receivedJsonObj.get("hostname").getAsString());
			serverInfo.setRemotePort(receivedJsonObj.get("port").getAsInt());
			serverInfoList.add(serverInfo);
		}
		// This is a known server, update server load info
		else
		{
			serverInfo.setServerLoad(receivedJsonObj.get("load").getAsInt());
		}

		return false;
	}

	private boolean processInvalidMsg(JsonObject receivedJsonObj)
	{
		String errorInfo = receivedJsonObj.get("info").getAsString();

		log.info(errorInfo);
		
		if (errorInfo.equals(JsonMessage.UNAUTHENTICATED_SERVER) || 
			errorInfo.equals(JsonMessage.REPEATED_AUTHENTICATION))
		{
			return true;
		}
		
		return false;
	}
	
	private boolean isUserInfoMsgValid(Connection con, JsonObject receivedJsonObj)
	{
		InvalidMsg invalidMsg = new InvalidMsg();
		
		if (!receivedJsonObj.has("username"))
		{
			invalidMsg.setInfo("Message must contain field username");
			con.writeMsg(invalidMsg.toJsonString());
			
			return false;
		}
		else if (!receivedJsonObj.has("secret"))
		{
			invalidMsg.setInfo("Message must contain field secret");
			con.writeMsg(invalidMsg.toJsonString());
			
			return false;
		}
		else
		{
			return true;
		}
	}

	private boolean processInvalidCommand(Connection con, JsonObject receivedJsonObj)
	{
		String command = receivedJsonObj.get("command").getAsString();
		
		InvalidMsg invalidMsg = new InvalidMsg();
		invalidMsg.setInfo("Invalid command: " + command);
		con.writeMsg(invalidMsg.toJsonString());
		
		return true;
	}
	
	private BroadcasterInfo findServer(String id)
	{
		for (BroadcasterInfo serverInfo : serverInfoList)
		{
			if (serverInfo.getId().equals(id))
			{
				return serverInfo;
			}
		}

		return null;
	}

	private BroadcasterInfo loadBalance()
	{
		int minLoad = CLIENT_CONNECTION_UPPER_LIMIT;
		BroadcasterInfo clusterWithLowestLoad = null;
		
		for (BroadcasterInfo cluster : serverInfoList)
		{
			if (cluster.getServerLoad() < minLoad)
			{
				minLoad = cluster.getServerLoad();
				clusterWithLowestLoad = cluster;
			}
		}
		
		return clusterWithLowestLoad;
	}
	
	private boolean isServerAuthenticated(Connection con)
	{
		for (Connection connection : serverConnectionList)
		{
			if (con.getSocket().getPort() == connection.getSocket().getPort())
			{
				InvalidMsg invalidMsg = new InvalidMsg();
				invalidMsg.setInfo(JsonMessage.REPEATED_AUTHENTICATION);
				con.writeMsg(invalidMsg.toJsonString());
				
				return true;
			}
		}
		
		return false;
	}

	private boolean hasClientInfo(String username, String secret)
	{
		return clientInfoList.containsKey(username) && clientInfoList.get(username).equals(secret);
	}
	
	private boolean containCommandField(Connection con, JsonObject receivedJsonObj)
	{
		if (!receivedJsonObj.has("command"))
		{
			InvalidMsg invalidMsg = new InvalidMsg();
			invalidMsg.setInfo("Message must contain field command");
			con.writeMsg(invalidMsg.toJsonString());
			
			return false;
		}
		
		return true;
	}
	
	public boolean doActivity()
	{
		/*
		 * do additional work here return true/false as appropriate
		 */
		log.info("Load balancer do activity");

		return false;
	}
	
	/*
	 * the connection has been closed
	 */
	@Override
	public void connectionClosed(Connection con)
	{
		super.connectionClosed(con);
		
		/*
		 * do additional things here
		 */
		if (!term)
		{
			serverConnectionList.remove(con);
		}
	}
	
	public void run()
	{
		log.info("using activity interval of " + Settings.getActivityInterval() + " milliseconds");

		while (!term)
		{
			// do something with 5 second intervals in between
			try
			{
				Thread.sleep(Settings.getActivityInterval());
			}
			catch (InterruptedException e)
			{
				log.info("received an interrupt, system is shutting down");
				
				break;
			}
			if (!term)
			{
				log.debug("doing activity");
				
				term = doActivity();
			}

		}
		
		log.info("closing " + serverConnectionList.size() + " connections");

		// clean up
		for (Connection connection : serverConnectionList)
		{
			connection.closeCon();
		}

		listener.setTerm(true);
	}
}
