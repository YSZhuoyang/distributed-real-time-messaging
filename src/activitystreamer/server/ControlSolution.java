package activitystreamer.server;

import java.io.IOException;
import java.net.Socket;
import java.util.ArrayList;
import java.util.HashMap;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonSyntaxException;

import Message.*;
import activitystreamer.util.Settings;

public class ControlSolution extends Control
{
	private static final Logger log = LogManager.getLogger();

	/*
	 * additional variables as needed
	 */
	private ArrayList<Connection> serverConnectionList = new ArrayList<>();
	private ArrayList<Connection> clientConnectionList = new ArrayList<>();

	// All servers except itself
	private ArrayList<ServerInfo> serverInfoList = new ArrayList<>();
	private HashMap<String, String> clientInfoList = new HashMap<>();
	private ArrayList<LockInfo> lockInfoList = new ArrayList<>();

	private String id;

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
		}

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

		// Send authentication message
		AuthMsg authJson = new AuthMsg();
		authJson.setSecret(Settings.getSecret());

		String authJsonStr = authJson.toJsonString();
		con.writeMsg(authJsonStr);

		serverConnectionList.add(con);
		
		// Broadcast server announce immediately
		doActivity();

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
		con.closeCon();
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
		log.debug("Server Receieved: " + msg);
		
		JsonObject receivedJsonObj;
		
		try
		{
			receivedJsonObj = new Gson().fromJson(msg, JsonObject.class);
		}
		catch (JsonSyntaxException e)
		{
			log.debug("Server receiving msg failed. Not json format: " + e.getMessage());
			
			con.closeCon();
			
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

			case JsonMessage.AUTHENTICATION_FAIL:
				return processAuthFailedMsg(con, receivedJsonObj);

			case JsonMessage.LOGOUT:
				return processLogoutMsg(con, receivedJsonObj);

			case JsonMessage.INVALID_MESSAGE:
				return processInvalidMsg(receivedJsonObj);
				
			case JsonMessage.ACTIVITY_MESSAGE:
				return processActivityMsg(con, receivedJsonObj);
				
			case JsonMessage.ACTIVITY_BROADCAST:
				return processActivityBroadcastMsg(con, receivedJsonObj);

			case JsonMessage.SERVER_ANNOUNCE:
				return processServerAnnounceMsg(con, receivedJsonObj);

			case JsonMessage.LOCK_REQUEST:
				return processLockRequestMsg(con, receivedJsonObj);

			case JsonMessage.LOCK_DENIED:
				return processLockDeniedMsg(con, receivedJsonObj);

			case JsonMessage.LOCK_ALLOWED:
				return processLockAllowedMsg(con, receivedJsonObj);

			default:
				return processInvalidCommand(con, receivedJsonObj);
		}
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
		// Broadcast server announce
		ServerAnnounceMsg serverAnnounceMsg = new ServerAnnounceMsg();
		serverAnnounceMsg.setHostname(Settings.getLocalHostname());
		serverAnnounceMsg.setId(id);
		serverAnnounceMsg.setLoad(clientConnectionList.size());
		serverAnnounceMsg.setPort(Settings.getLocalPort());

		String serverAnnounceJsonStr = serverAnnounceMsg.toJsonString();

		// Broad server announce to adjacent servers
		broadcastToAllOtherServers(serverAnnounceJsonStr);

		log.info("Server announcement sent");

		return false;
	}
	
	/*
	 * Other methods as needed
	 */
	private boolean processLogoutMsg(Connection con, JsonObject receivedJsonObj)
	{
		// Remove client from the client connection list
		Connection toBeDeleted = null;
		
		for (Connection connection : clientConnectionList)
		{
			if (connection.getSocket().getPort() == con.getSocket().getPort())
			{
				toBeDeleted = connection;
			}
		}
		
		if (toBeDeleted != null)
		{
			clientConnectionList.remove(toBeDeleted);
		}

		log.info("user logout");

		return true;
	}
	
	private boolean processAuthFailedMsg(Connection con, JsonObject receivedJsonObj)
	{
		log.info("Authentication failed");
		
		serverConnectionList.remove(con);

		return true;
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
	
	private boolean processActivityBroadcastMsg(Connection con, JsonObject receivedJsonObj)
	{
		log.debug("Activity broadcast message received from port: " + con.getSocket().getPort());

		// Validate activity message
		if (!isActivityMsgValid(con, receivedJsonObj))
		{
			return true;
		}
		
		String jsonStr = new Gson().toJson(receivedJsonObj);
		
		broadcastToAllClients(jsonStr);
		forwardToOtherServers(con, jsonStr);
		
		return false;
	}
	
	private boolean processActivityMsg(Connection con, JsonObject receivedJsonObj)
	{
		log.info("Activity message received from port: " + con.getSocket().getPort());
		
		// Validate activity message
		if (!isActivityMsgValid(con, receivedJsonObj))
		{
			return true;
		}
		
		// Check username and secret!!
		String username = receivedJsonObj.get("username").getAsString();
		String secret = receivedJsonObj.get("secret").getAsString();
		
		if (username.equals(JsonMessage.ANONYMOUS_USERNAME) || hasClientInfo(username, secret))
		{
			log.debug("Broadcast activity message received from client");

			// Convert it to activity broadcast message
			JsonObject actJsonObj = receivedJsonObj.get("activity").getAsJsonObject();
			String content = actJsonObj.get("object").getAsString();
			
			ActBroadMsg actBroadMsg = new ActBroadMsg();
			actBroadMsg.setActor(username);
			actBroadMsg.setObject(content);
			
			String activityJsonStr = actBroadMsg.toJsonString();
			
			broadcastToAllClients(activityJsonStr);
			broadcastToAllOtherServers(activityJsonStr);
			
			return false;
		}
		// No need to consider the case where user is not logged in since
		// GUI will display only after user successfully log in
		else
		{
			log.info("Invalid activity message received, auth failed message sent");
			
			AuthFailMsg authFailMsg = new AuthFailMsg();
			authFailMsg.setInfo("username and secret do not match");
			
			con.writeMsg(authFailMsg.toJsonString());
			
			return true;
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

			return false;
		}
		// Redirect to another server and close the connection
		else
		{
			log.info("Redirected");

			RedirectMsg redirectMsg = new RedirectMsg();
			redirectMsg.setHost(serverInfo.getRemoteHostname());
			redirectMsg.setPort(serverInfo.getRemotePort());

			String redirectMsgJsonStr = redirectMsg.toJsonString();
			con.writeMsg(redirectMsgJsonStr);

			return true;
		}
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

		// Check whether username already exists
		if (clientInfoList.containsKey(username))
		{
			log.info("Register failed. Username already exists!");

			RegisterFailedMsg registerFailedMsg = new RegisterFailedMsg();
			registerFailedMsg.setInfo(username + " is already registered in the system");

			String registFailedJsonStr = registerFailedMsg.toJsonString();
			con.writeMsg(registFailedJsonStr);

			return true;
		}
		// Only root server, no other server exists
		else if (serverConnectionList.size() == 0)
		{
			log.info("Register_Success");

			// Send register success message
			RegistSuccMsg registerSuccMsg = new RegistSuccMsg();
			registerSuccMsg.setInfo("register success for " + username);

			String registSuccJsonStr = registerSuccMsg.toJsonString();
			con.writeMsg(registSuccJsonStr);

			// Add client info
			clientInfoList.put(username, secret);
			con.closeCon();
			
			return true;
		}

		// Create a new server list managing lock info
		LockInfo lockInfo = new LockInfo(username, secret);
		lockInfo.setConnection(con);

		lockInfoList.add(lockInfo);

		// Broadcast lock request
		LockRequestMsg lockRequestMsg = new LockRequestMsg();
		lockRequestMsg.setUsername(username);
		lockRequestMsg.setSecret(secret);

		String lockRequestJsonStr = lockRequestMsg.toJsonString();
		broadcastToAllOtherServers(lockRequestJsonStr);
		
		log.info("Lock request sent");

		return false;
	}

	private boolean processAuthMsg(Connection con, JsonObject receivedJsonObj)
	{
		if (!receivedJsonObj.has("secret"))
		{
			log.info("Auth faield");

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
		ServerInfo serverInfo = findServer(id);

		// This is a new server
		if (serverInfo == null)
		{
			serverInfo = new ServerInfo();
			serverInfo.setId(id);
			serverInfo.setServerLoad(receivedJsonObj.get("load").getAsInt());
			serverInfo.setRemoteHostname(receivedJsonObj.get("hostname").getAsString());
			serverInfo.setRemotePort(receivedJsonObj.get("port").getAsInt());
			serverInfo.setConnected(true);
			serverInfo.setConnection(con);
			serverInfoList.add(serverInfo);
		}
		// This is a known server, update server load info
		else
		{
			serverInfo.setServerLoad(receivedJsonObj.get("load").getAsInt());
		}

		// Inform others to update the server list
		ServerAnnounceMsg serverAnnounceMsg = new ServerAnnounceMsg();
		serverAnnounceMsg.setId(serverInfo.getId());
		serverAnnounceMsg.setHostname(serverInfo.getRemoteHostname());
		serverAnnounceMsg.setLoad(serverInfo.getServerLoad());
		serverAnnounceMsg.setPort(serverInfo.getRemotePort());

		// Send to adjacent servers
		String serverAnnounceJsonStr = serverAnnounceMsg.toJsonString();
		forwardToOtherServers(con, serverAnnounceJsonStr);

		return false;
	}

	private boolean processLockAllowedMsg(Connection con, JsonObject receivedJsonObj)
	{
		String username = receivedJsonObj.get("username").getAsString();
		String secret = receivedJsonObj.get("secret").getAsString();
		String id = receivedJsonObj.get("server").getAsString();
		LockInfo lockInfoToBeDeleted = null;

		// Each lockInfo is bound with a user who is trying to register on this server
		for (LockInfo lockInfo : lockInfoList)
		{
			// This server is the one where this user is trying to register since its lock info was found
			if (username.equals(lockInfo.getUsername()) && secret.equals(lockInfo.getSecret()))
			{
				log.info("Lock allowed received");

				lockInfo.addAllowedServer(id);

				if (lockInfo.lockAllowedMsgReceivedFromAllServers(serverInfoList))
				{
					log.info("Register_Success");

					// Send register success message
					Connection clientConnection = lockInfo.getConnection();

					RegistSuccMsg registerSuccMsg = new RegistSuccMsg();
					registerSuccMsg.setInfo("register success for " + username);

					String registSuccJsonStr = registerSuccMsg.toJsonString();
					clientConnection.writeMsg(registSuccJsonStr);

					// Add client info
					clientInfoList.put(username, secret);
					lockInfoList.remove(lockInfo);

					lockInfoToBeDeleted = lockInfo;
					
					// Delete lock info after user successfully registered
					lockInfoList.remove(lockInfoToBeDeleted);
					clientConnection.closeCon();
					
					return false;
				}
			}
		}

		String lockAllowedJsonStr = new Gson().toJson(receivedJsonObj);
		forwardToOtherServers(con, lockAllowedJsonStr);

		return false;
	}

	private boolean processLockDeniedMsg(Connection con, JsonObject receivedJsonObj)
	{
		log.info("Lock denied received");
		
		String username = receivedJsonObj.get("username").getAsString();
		String secret = receivedJsonObj.get("secret").getAsString();

		boolean registerDenied = false;
		LockInfo lockInfoToBeDeleted = null;

		for (LockInfo lockInfo : lockInfoList)
		{
			// Server where a user is registering received a lock denied message
			if (lockInfo.getUsername().equals(username) && lockInfo.getSecret().equals(secret))
			{
				Connection clientConnection = lockInfo.getConnection();

				// Send register failed info
				RegisterFailedMsg registerFailedMsg = new RegisterFailedMsg();
				registerFailedMsg.setInfo("username already exists");

				String registerFailedJsonStr = registerFailedMsg.toJsonString();
				clientConnection.writeMsg(registerFailedJsonStr);

				clientConnectionList.remove(clientConnection);
				registerDenied = true;
				lockInfoToBeDeleted = lockInfo;

				break;
			}
		}

		if (registerDenied)
		{
			log.info("Register denied");
			
			lockInfoList.remove(lockInfoToBeDeleted);
		}
		// The user is trying to register on another server, and this server received 
		// a lock denied message
		else
		{
			String lockdeniedJsonStr = new Gson().toJson(receivedJsonObj);
			forwardToOtherServers(con, lockdeniedJsonStr);
			deleteClientInfo(username, secret);
		}
		
		return false;
	}

	private boolean processLockRequestMsg(Connection con, JsonObject receivedJsonObj)
	{
		log.info("Lock request received");
		
		String secret = receivedJsonObj.get("secret").getAsString();
		String username = receivedJsonObj.get("username").getAsString();

		// Consider both user names registered and waiting to be registered (which are
		// people who is trying to register). This prevents more than one users register
		// on different servers with the same user name at the same time.
		//if (clientInfoList.containsKey(username) || lockInfoContainsUsername(username))
		/*-----------------------------------------------------------------------------*/
		// Only consider user names registered according to the specifications
		if (clientInfoList.containsKey(username))
		{
			log.info("Lock denied sent");
			
			LockDeniedMsg lockDeniedMsg = new LockDeniedMsg();
			lockDeniedMsg.setUsername(username);
			lockDeniedMsg.setSecret(secret);

			String lockDeniedJsonStr = lockDeniedMsg.toJsonString();

			// Broadcast lock denied message
			broadcastToAllOtherServers(lockDeniedJsonStr);
		}
		else
		{
			LockAllowedMsg lockAllowedMsg = new LockAllowedMsg();
			lockAllowedMsg.setUsername(username);
			lockAllowedMsg.setSecret(secret);
			lockAllowedMsg.setServer(id);

			String lockAllowedJsonStr = lockAllowedMsg.toJsonString();
			broadcastToAllOtherServers(lockAllowedJsonStr);

			clientInfoList.put(username, secret);
		}
		
		LockRequestMsg lockRequestMsg = new LockRequestMsg();
		lockRequestMsg.setUsername(username);
		lockRequestMsg.setSecret(secret);
		
		forwardToOtherServers(con, lockRequestMsg.toJsonString());

		return false;
	}

	private void broadcastToAllOtherServers(String jsonStr)
	{
		for (Connection con : serverConnectionList)
		{
			con.writeMsg(jsonStr);
		}
	}
	
	private void forwardToOtherServers(Connection current, String jsonStr)
	{
		for (Connection con : serverConnectionList)
		{
			if (current.getSocket().getPort() != con.getSocket().getPort())
			{
				con.writeMsg(jsonStr);
			}
		}
	}

	private void broadcastToAllClients(String jsonStr)
	{
		for (Connection con : clientConnectionList)
		{
			con.writeMsg(jsonStr);
		}
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
	
	private boolean isActivityMsgValid(Connection con, JsonObject receivedJsonObj)
	{
		if (!receivedJsonObj.has("activity"))
		{
			InvalidMsg invalidMsg = new InvalidMsg();
			invalidMsg.setInfo("Message must contain field activity");
			con.writeMsg(invalidMsg.toJsonString());
			
			return false;
		}
		
		return true;
	}
	
	private boolean processInvalidCommand(Connection con, JsonObject receivedJsonObj)
	{
		String command = receivedJsonObj.get("command").getAsString();
		
		InvalidMsg invalidMsg = new InvalidMsg();
		invalidMsg.setInfo("Invalid command: " + command);
		con.writeMsg(invalidMsg.toJsonString());
		
		return true;
	}
	
	private void deleteClientInfo(String u, String s)
	{
		String secret = clientInfoList.get(u);

		if (secret != null && secret.equals(s))
		{
			clientInfoList.remove(u);
		}
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
			if (server.getServerLoad() < clientConnectionList.size())
			{
				return server;
			}
		}

		return null;
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
	
	private boolean lockInfoContainsUsername(String username)
	{
		for (LockInfo lockInfo : lockInfoList)
		{
			if (lockInfo.getUsername().equals(username))
			{
				return true;
			}
		}
		
		return false;
	}

	private boolean hasClientInfo(String username, String secret)
	{
		return clientInfoList.containsKey(username) && clientInfoList.get(username).equals(secret);
	}
}
