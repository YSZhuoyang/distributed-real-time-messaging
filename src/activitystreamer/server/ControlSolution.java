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
		log.debug("Server Receieved: " + msg);

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

			case JsonMessage.AUTHENTICATION_FAIL:
				log.info("Authentication failed");
				
				serverConnectionList.remove(con);

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
				log.info("Activity message received from port: " + con.getSocket().getPort());
				
				if (clientConnectionList.contains(con))
				{
					System.out.println("From client");
				}
				
				// Check username and secret!!
				String username = receivedJsonObj.get("username").getAsString();
				String secret = receivedJsonObj.get("secret").getAsString();
				
				if (hasClientInfo(username, secret))
				{
					// Convert it to activity broadcast message
					JsonObject actJsonObj = receivedJsonObj.get("activity").getAsJsonObject();
					String content = actJsonObj.get("object").getAsString();
					
					ActBroadMsg actBroadMsg = new ActBroadMsg();
					actBroadMsg.setActor(username);
					actBroadMsg.setObject(content);
					
					String activityJsonStr = actBroadMsg.toJsonString();
					
					log.debug("Broadcast activity message received from client");

					broadcastToAllClients(activityJsonStr);
					broadcastToAllOtherServers(activityJsonStr);

					// send
					//return processClientMsg(con,receivedJsonObj);
				}
				else
				{
					InvalidMsg invalidMsg = new InvalidMsg();
					invalidMsg.setInfo("User name or secret incorrect");
					
					con.writeMsg(invalidMsg.toJsonString());
				}
				
				break;
				
			case JsonMessage.ACTIVITY_BROADCAST:
				log.info("Activity broadcast message received from port: " + con.getSocket().getPort());
				
				if (serverConnectionList.contains(con))
				{
					System.out.println("From server");
				}
				
				log.debug("Broadcast activity message to other connected server and clients");

				broadcastToAllClients(msg);
				forwardToOtherServers(con, msg);
				
				break;

			case JsonMessage.SERVER_ANNOUNCE:
				return processServerAnnounceMsg(con, receivedJsonObj);

			case JsonMessage.LOCK_REQUEST:
				return processLockRequestMsg(con, receivedJsonObj);

			case JsonMessage.LOCK_DENIED:
				return processLockDeniedMsg(con, receivedJsonObj);

			case JsonMessage.LOCK_ALLOWED:
				return processLockAllowedMsg(con, receivedJsonObj);

			default:
				break;
		}
		
		return false;
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
	private boolean processLoginMsg(Connection con, JsonObject receivedJsonObj)
	{
		String secret = receivedJsonObj.get("secret").getAsString();
		String username = receivedJsonObj.get("username").getAsString();

		// Secret is not correct, login failed
		if (!hasClientInfo(username, secret))
		{
			log.info("attempt to login with a wrong secret");

			LoginFailedMsg loginFailedMsg = new LoginFailedMsg();
			loginFailedMsg.setInfo("Register failed, secret does not exist!");

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
			redirectMsg.setPort("" + serverInfo.getRemotePort());

			String redirectMsgJsonStr = redirectMsg.toJsonString();
			con.writeMsg(redirectMsgJsonStr);

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
			log.info("Register failed. Username already exists!");

			RegisterFailedMsg registerFailedMsg = new RegisterFailedMsg();
			registerFailedMsg.setInfo(username + " is already registered in the system");

			String registFailedJsonStr = registerFailedMsg.toJsonString();
			con.writeMsg(registFailedJsonStr);

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

		String id = receivedJsonObj.get("id").getAsString();
		ServerInfo serverInfo = findServer(id);

		// This is a new server
		if (serverInfo == null)
		{
			serverInfo = new ServerInfo();
			serverInfo.setId(id);
			serverInfo.setServerLoad(receivedJsonObj.get("load").getAsInt());
			serverInfo.setRemoteHostname(receivedJsonObj.get("host").getAsString());
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

		String serverAnnounceJsonStr = serverAnnounceMsg.toJsonString();

		log.debug("Connection number: " + serverConnectionList.size());

		// Send to adjacent servers
		forwardToOtherServers(con, serverAnnounceJsonStr);

		return false;
	}

	// Server processing the activity message before broadcasting it.
	/*private boolean processClientMsg(Connection con, JsonObject activityObj)
	{

		String thisUsername = null;
		String thisSecret = null;
		thisUsername = activityObj.get("username").toString();
		thisSecret = activityObj.get("secret").toString();

		if (activityObj.get("username").equals("anonymous") || hasClientInfo(thisUsername, thisSecret))
		{

			ActBroadMsg actBroadMsg = new ActBroadMsg();
			/*
			 * Activity activity =
			 * activity().object(activityObj.get("object").getAsString())
			 * .authenticatetd_user(thisUsername) .get();
			 */
			// actBroadMsg.setActivity(activity);
			/*log.debug("Recieved a new message from the client: " + thisUsername + "with the content: " + ""
					+ actBroadMsg.toJsonString());
			broadcastToAllServers(actBroadMsg.toJsonString());
		}
		else
		{
			AuthFailMsg authFailedMsg = new AuthFailMsg();
			authFailedMsg.setInfo("the supplied secret is incorrect: " + thisSecret);
			String authFailedJsonStr = authFailedMsg.toJsonString();
			con.writeMsg(authFailedJsonStr);
		}
		return false;
	}*/

	private boolean processLockAllowedMsg(Connection con, JsonObject receivedJsonObj)
	{
		String username = receivedJsonObj.get("username").getAsString();
		String secret = receivedJsonObj.get("secret").getAsString();
		String id = receivedJsonObj.get("server").getAsString();

		boolean registerSucceed = false;
		boolean registeringOnThisServer = false;
		LockInfo lockInfoToBeDeleted = null;

		// Each lockInfo is bound with a user who is trying to register on this server
		for (LockInfo lockInfo : lockInfoList)
		{
			if (username.equals(lockInfo.getUsername()) && secret.equals(lockInfo.getSecret()))
			{
				log.info("Lock allowed received");

				lockInfo.addAllowedServer(id);
				registeringOnThisServer = true;

				if (lockInfo.lockAllowedMsgReceivedFromAllServers(serverInfoList))
				{
					log.info("Register_Success");

					// Register success
					Connection clientConnection = lockInfo.getConnection();

					RegistSuccMsg registerSuccMsg = new RegistSuccMsg();
					registerSuccMsg.setInfo("register success for " + username);

					String registSuccJsonStr = registerSuccMsg.toJsonString();
					clientConnection.writeMsg(registSuccJsonStr);

					// Add client info and client connection
					clientInfoList.put(username, secret);
					lockInfoList.remove(lockInfo);

					lockInfoToBeDeleted = lockInfo;
					registerSucceed = true;

					// Find a server with less client connections
					ServerInfo serverInfo = loadBalance();

					// Connect to this server
					if (serverInfo == null)
					{
						clientConnectionList.add(clientConnection);
					}
					// Redirect to another server and close the client
					// connection
					else
					{
						log.info("Redirected");

						RedirectMsg redirectMsg = new RedirectMsg();
						redirectMsg.setHost(serverInfo.getRemoteHostname());
						redirectMsg.setPort("" + serverInfo.getRemotePort());
						String redirectMsgJsonStr = redirectMsg.toJsonString();

						clientConnection.writeMsg(redirectMsgJsonStr);
						clientConnection.closeCon();
					}

					break;
				}
			}
		}

		if (!registeringOnThisServer)
		{
			String lockAllowedJsonStr = new Gson().toJson(receivedJsonObj);
			forwardToOtherServers(con, lockAllowedJsonStr);
		}
		// Delete lock info after user successfully registered
		else if (registerSucceed)
		{
			lockInfoList.remove(lockInfoToBeDeleted);
		}

		return false;
	}

	private boolean processLockDeniedMsg(Connection con, JsonObject receivedJsonObj)
	{
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
			lockInfoList.remove(lockInfoToBeDeleted);
		}
		// Server where the user is not registering received a lock denied
		// message
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
		String secret = receivedJsonObj.get("secret").getAsString();
		String username = receivedJsonObj.get("username").getAsString();

		if (clientInfoList.containsKey(username))
		{
			LockDeniedMsg lockDeniedMsg = new LockDeniedMsg();
			lockDeniedMsg.setUsername(username);
			lockDeniedMsg.setSecret(secret);

			String lockDeniedJsonStr = lockDeniedMsg.toJsonString();

			// Broadcast lock denied message
			broadcastToAllOtherServers(lockDeniedJsonStr);
			//broadcastToAllServers(lockDeniedJsonStr);
		}
		else
		{
			LockAllowedMsg lockAllowedMsg = new LockAllowedMsg();
			lockAllowedMsg.setUsername(username);
			lockAllowedMsg.setSecret(secret);
			lockAllowedMsg.setServer(id);

			String lockAllowedJsonStr = lockAllowedMsg.toJsonString();
			broadcastToAllOtherServers(lockAllowedJsonStr);
			//con.writeMsg(lockAllowedJsonStr);

			clientInfoList.put(username, secret);
		}

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
			else
			{
				log.debug("Same Server!!!!!!!!");
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
	
	private void deleteClientInfo(String u, String s)
	{
		String secret = clientInfoList.get(u);

		if (secret.equals(s))
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
			if (server.getServerLoad() < clientConnectionList.size() - 1)
			{
				return server;
			}
		}

		return null;
	}

	private boolean hasClientInfo(String username, String secret)
	{
		return clientInfoList.containsKey(username) || clientInfoList.get(username).equals(secret);
	}
}
