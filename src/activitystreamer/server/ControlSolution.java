package activitystreamer.server;

import java.io.IOException;
import java.net.Socket;
import java.util.ArrayList;
import java.util.HashMap;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.google.gson.Gson;
import com.google.gson.JsonObject;

import Message.*;
import activitystreamer.util.Settings;

public class ControlSolution extends Control
{
	private static final Logger log = LogManager.getLogger();

	/*
	 * additional variables as needed
	 */
	private static ArrayList<Connection> serverConnectionList = new ArrayList<>();
	private static ArrayList<Connection> clientConnectionList = new ArrayList<>();
	
	// All servers except itself
	private static ArrayList<ServerInfo> serverInfoList = new ArrayList<>();
	private static HashMap<String, Boolean> lockInfoList = new HashMap<>();
	private static HashMap<String, String> clientInfoList = new HashMap<>();

	// Assuming that id is secret
	private static int numClientConnections = 0;
	private static String id;
	//private static boolean lockAllowed;
	//private static boolean lockDenied;
	
	// since control and its subclasses are singleton, we get the singleton this
	// way
	public static ControlSolution getInstance()
	{
		if (control == null)
		{
			control = new ControlSolution();
		}

		return (ControlSolution) control;
	}

	public ControlSolution()
	{
		super();
		
		id = Settings.nextSecret();

		/*
		 * Do some further initialization here if necessary
		 */

		// This is the root server
		if (Settings.getRemoteHostname() == null)
		{
			// generate secrete keys here
			String secret = Settings.nextSecret();
			Settings.setSecret(secret);

			log.info("The secret key of this root server is: " + secret);
		}
		// else is not the root server;check if the secret is correct or the
		// command is valid
		else
		{
			// check if we should initiate a connection and do so if necessary
			initiateConnection();
			
			ServerInfo serverInfo = new ServerInfo();
			serverInfo.setId(id);
			serverInfo.setRemoteHostname(Settings.getLocalHostname());
			serverInfo.setRemotePort(Settings.getLocalPort());
			serverInfo.setServerLoad(0);
			
			serverInfoList.add(serverInfo);
		}
		
		//serverInfoList.add(serverInfo);
		//log.debug("Server counter: " + serverInfoList.size());
		log.debug("Secret: " + Settings.getSecret());
		
		
		// start the server's activity loop
		// it will call doActivity every few seconds
		start();
	}

	/*
	 * a new incoming connection
	 */
	@Override
	public Connection incomingConnection(Socket s) throws IOException
	{
		Connection con = super.incomingConnection(s);

		/*
		 * do additional things here
		 */

		return con;
	}

	/*
	 * a new outgoing connection
	 */
	@Override
	public Connection outgoingConnection(Socket s) throws IOException
	{
		/*
		 * do additional things here
		 */
		Connection con = super.outgoingConnection(s);
		
		AuthMsg authJson = new AuthMsg();
		authJson.setSecret(Settings.getSecret());
		String authJsonStr = authJson.toJsonString();
		con.writeMsg(authJsonStr);
		serverConnectionList.add(con);
		
		return con;
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
	}

	/*
	 * process incoming msg, from connection con return true if the connection
	 * should be closed, false otherwise
	 */
	@Override
	public synchronized boolean process(Connection con, String msg)
	{
		/*
		 * do additional work here return true/false as appropriate
		 */

		log.debug("Receieved: " + msg);

		JsonObject receivedJsonObj = new Gson().fromJson(msg, JsonObject.class);
		String msgType = receivedJsonObj.get("command").getAsString();

		switch (msgType)
		{
			case JsonMessage.LOGIN:
				return processLoginMsg(con, receivedJsonObj);

			case JsonMessage.REGISTER:
				return processRegisterMsg(con, receivedJsonObj);

			case JsonMessage.AUTHENTICATE:
				return processAuthMsg(con, receivedJsonObj);

			case JsonMessage.AUTHTENTICATION_FAIL:
				log.info("Authentication failed");

				return true;

			case JsonMessage.LOGOUT:
				// Remove client from the client connection list
				clientConnectionList.remove(con);

				log.info("user logout");

				return true;

			/*case "INVALID_MESSAGE":
				if (receivedJsonObj.get("command").getAsString() == "invalid_message")
					;
				InvalidMsg invalidMsg = new InvalidMsg();
				invalidMsg.setInfo("Invalid_Message");
				String invalidMessage = invalidMsg.toJsonString();
				con.writeMsg(invalidMessage);

				break;
			*/
				
			case JsonMessage.ACTIVITY_MESSAGE:
				// send

				break;

			case JsonMessage.SERVER_ANNOUNCE:
				return processServerAnnounceMsg(con, receivedJsonObj);

			case JsonMessage.ACTIVITY_BROADCAST:
				// broadcast

				break;

			case JsonMessage.LOCK_REQUEST:
				return processLockRequestMsg(con, receivedJsonObj);

			case JsonMessage.LOCK_DENIED:
				// ...

				break;

			case JsonMessage.LOCK_ALLOWED:
				// ...
				id = receivedJsonObj.get("id").getAsString();
				
				for (ServerInfo info : serverInfoList)
				{
					if (id.equals(info.getId()))
					{
						lockInfoList.put(id, true);
						
						break;
					}
				}

				break;

			default:
				break;
		}
		
		return false;

	}

	private boolean processLockRequestMsg(Connection con, JsonObject receivedJsonObj)
	{
		String secret = receivedJsonObj.get("secret").getAsString();
		String username = receivedJsonObj.get("username").getAsString();
		
		if (hasClientInfo(username, secret))
		{
			LockDeniedMsg lockDeniedMsg = new LockDeniedMsg();
			lockDeniedMsg.setUsername(username);
			lockDeniedMsg.setSecret(secret);
			
			String lockDeniedJsonStr = lockDeniedMsg.toJsonString();
			
			con.writeMsg(lockDeniedJsonStr);
		}
		else
		{
			LockAllowedMsg lockAllowedMsg = new LockAllowedMsg();
			lockAllowedMsg.setUsername(username);
			lockAllowedMsg.setSecret(secret);
			
			// How to get server secret
			//lockAllowedMsg.setServer(s);\
		}
		
		return false;
	}

	private boolean hasClientInfo(String username, String secret)
	{
		return clientInfoList.containsKey(username) || clientInfoList.get(username).equals(secret);
	}

	/*
	 * Called once every few seconds Return true if server should shut down,
	 * false otherwise
	 */
	@Override
	public boolean doActivity()
	{
		/*
		 * do additional work here return true/false as appropriate
		 */
		ServerAnnounceMsg serverAnnounceMsg = new ServerAnnounceMsg();
		serverAnnounceMsg.setHostname(Settings.getLocalHostname());
		serverAnnounceMsg.setId(id);
		serverAnnounceMsg.setLoad(numClientConnections);
		serverAnnounceMsg.setPort(numClientConnections);
		
		String serverAnnounceJsonStr = serverAnnounceMsg.toJsonString();
		
		for (Connection con : serverConnectionList)
		{
			con.writeMsg(serverAnnounceJsonStr);
		}
		
		log.info("Server announcement sent");
		
		return false;
	}

	/*
	 * Other methods as needed
	 */
	private boolean processLoginMsg(Connection con, JsonObject receivedJsonObj)
	{
		String secret = receivedJsonObj.get("secret").getAsString();
		String username = receivedJsonObj.get("username").getAsString();

		// Secret is not correct, login failed
		if (!hasClientInfo(username, secret))
		{
			LoginFailedMsg loginFailedMsg = new LoginFailedMsg();
			loginFailedMsg.setInfo("Register failed, secret does not exist!");
			String registFailedJsonStr = loginFailedMsg.toJsonString();

			log.info("attempt to login with a wrong secret");

			con.writeMsg(registFailedJsonStr);

			return true;
		}
		
		log.info("logged in as user " + username);

		LoginSuccMsg loginSuccMsg = new LoginSuccMsg();
		loginSuccMsg.setInfo("Login successful");
		String loginSuccJsonStr = loginSuccMsg.toJsonString();
		con.writeMsg(loginSuccJsonStr);

		// Find a server with less client connection
		ServerInfo serverInfo = loadBalance();

		// Connect to this server
		if (serverInfo == null)
		{
			clientConnectionList.add(con);
			numClientConnections++;

			return false;
		}
		// Connect to another server
		else
		{
			log.info("Redirected");

			RedirectMsg redirectMsg = new RedirectMsg();
			redirectMsg.setHost(serverInfo.getRemoteHostname());
			redirectMsg.setPort("" + serverInfo.getRemotePort());
			String redirectMsgJsonStr = redirectMsg.toJsonString();
			con.writeMsg(redirectMsgJsonStr);

			// Wait until writeMsg is done?
			return true;
		}
	}
	
	private boolean processRegisterMsg(Connection con, JsonObject receivedJsonObj)
	{
		String secret = receivedJsonObj.get("secret").getAsString();
		String username = receivedJsonObj.get("username").getAsString();

		// Check whether username already exists
		if (clientInfoList.containsKey(username))
		{
			log.info("Register failed. Secret already exists!");

			RegisterFailedMsg registerFailedMsg = new RegisterFailedMsg();
			registerFailedMsg.setInfo(username + " is already registered in the system");
			String registFailedJsonStr = registerFailedMsg.toJsonString();
			con.writeMsg(registFailedJsonStr);

			return true;
		}

		// Send lock request
		//lockAllowed = false;
		//lockDenied = false;
		
		LockRequestMsg lockRequestMsg = new LockRequestMsg();
		lockRequestMsg.setUsername(username);
		lockRequestMsg.setSecret(secret);
		
		String lockRequestJsonStr = lockRequestMsg.toJsonString();
		
		for (Connection connection : serverConnectionList)
		{
			if (connection.getSocket().getLocalPort() != con.getSocket().getLocalPort())
			{
				connection.writeMsg(lockRequestJsonStr);
			}
		}
		
		// Wait until lock allowed from all other server is received
		// A feasible solution is to start a new thread for waiting and 
		// receiving lock msg
		/*while (!lockAllowed)
		{
			// When all lock allowed messages are received
			// get the lock
			if (lockInfoList.size() == serverInfoList.size())
			{
				lockAllowed = true;
			}
			
			if (lockDenied)
			{
				return true;
			}
			
			//receivingLockAllowedMsg();
		}*/
		
		clientInfoList.put(username, secret);
		RegistSuccMsg registerSuccMsg = new RegistSuccMsg();
		registerSuccMsg.setInfo("register succ for " + username);
		String registSuccJsonStr = registerSuccMsg.toJsonString();
		con.writeMsg(registSuccJsonStr);
		clientConnectionList.add(con);
		numClientConnections++;

		log.info("Register_Success");
		
		return false;
	}
	
	private boolean processAuthMsg(Connection con, JsonObject receivedJsonObj)
	{
		String secret = receivedJsonObj.get("secret").getAsString();
		
		// Connect with server
		if (!secret.equals(Settings.getSecret()))
		{
			log.info("Auth faield");
			
			AuthFailMsg authFailedMsg = new AuthFailMsg();
			authFailedMsg.setInfo("the supplied secret is incorrect: " + secret);
			String authFailedJsonStr = authFailedMsg.toJsonString();
			con.writeMsg(authFailedJsonStr);
			
			return true;
		}
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
		
		String id = receivedJsonObj.get("id").getAsString();
		ServerInfo serverInfo = findServer(id);
		
		if (serverInfo == null)
		{
			serverInfo = new ServerInfo();
			serverInfo.setId(id);
			serverInfo.setServerLoad(receivedJsonObj.get("load").getAsInt());
			serverInfo.setRemoteHostname(receivedJsonObj.get("host").getAsString());
			serverInfo.setRemotePort(receivedJsonObj.get("port").getAsInt());
			serverInfoList.add(serverInfo);
		}

		// Inform others to update the server list
		ServerAnnounceMsg serverAnnounceMsg = new ServerAnnounceMsg();
		serverAnnounceMsg.setId(serverInfo.getId());
		serverAnnounceMsg.setHostname(serverInfo.getRemoteHostname());
		serverAnnounceMsg.setLoad(serverInfo.getServerLoad());
		serverAnnounceMsg.setPort(serverInfo.getRemotePort());
		
		String serverAnnounceJsonStr = serverAnnounceMsg.toJsonString();
		
		log.debug("Connection number: " + serverConnectionList.size());
		
		for (Connection connection : serverConnectionList)
		{
			if (con.getSocket().getLocalPort() != connection.getSocket().getLocalPort())
			{
				connection.writeMsg(serverAnnounceJsonStr);
				
				log.debug("Forwarded!!!!!!!!!");
			}
			else
			{
				log.debug("Same server!!!!!!!!!");
			}
		}

		return false;
	}
	
	private boolean processLockAllowedMsg(Connection con, JsonObject receivedJsonObj)
	{
		
		
		return false;
	}

	private ServerInfo findServer(String id)
	{
		for (ServerInfo serverInfo : serverInfoList)
		{
			if (serverInfo.getId().equals(id))
			{
				return serverInfo;
			}
		}
		
		return null;
	}

	private ServerInfo loadBalance()
	{
		for (ServerInfo server : serverInfoList)
		{
			if (server.getServerLoad() < numClientConnections - 1)
			{
				return server;
			}
		}

		return null;
	}
}
