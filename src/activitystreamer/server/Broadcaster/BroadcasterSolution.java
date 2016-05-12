package activitystreamer.server.Broadcaster;

import java.io.IOException;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.ArrayList;

import javax.net.ssl.HandshakeCompletedEvent;
import javax.net.ssl.HandshakeCompletedListener;
import javax.net.ssl.SSLPeerUnverifiedException;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonSyntaxException;

import Message.*;
import activitystreamer.util.Connection;
import activitystreamer.util.Control;
import activitystreamer.util.Settings;


public class BroadcasterSolution extends Control
{
	private static final Logger log = LogManager.getLogger();
	private static final int SERVER_CONNECTION_UPPER_LIMIT = 5;
	
	/*
	 * additional variables as needed
	 */
	private ArrayList<Connection> serverConnectionList = new ArrayList<>();
	private ArrayList<Connection> clientConnectionList = new ArrayList<>();
	private ClusterConnection cluster_LoadBalancer;

	private String id;

	// since control and its subclasses are singleton, we get the singleton this
	// way
	public static synchronized BroadcasterSolution getInstance()
	{
		if (control == null)
		{
			control = new BroadcasterSolution();
		}

		return (BroadcasterSolution) control;
	}

	private BroadcasterSolution()
	{
		super();

		// start a listener
		listener = BroadcastListener.getInstance();
		id = Settings.nextSecret();

		/*
		 * Do some further initialization here if necessary
		 */
		if (Settings.getRemoteHostname() != null)
		{
			if (connectToCluster())
			{
				connectToLoadBalancer();
			}
		}
		else
		{
			connectToLoadBalancer();
		}
		
		// start the server's activity loop
		// it will call doActivity every few seconds
		start();
	}

	/*
	 * a new incoming connection
	 */
	@Override
	public ClusterConnection incomingConnection(Socket s) throws IOException
	{
		log.debug("incomming connection: " + Settings.socketAddress(s));

		ClusterConnection con = new ClusterConnection(s);
		
		return con;
	}

	/*
	 * a new outgoing connection
	 */
	public void outgoingConnection(Socket s, boolean toLoadBalancer) throws IOException
	{
		/*
		 * do additional things here
		 */
		ClusterConnection con = new ClusterConnection(s);

		// Send authentication message
		AuthMsg authJson = new AuthMsg();
		authJson.setSecret(Settings.getSecret());

		String authJsonStr = authJson.toJsonString();
		con.writeMsg(authJsonStr);

		if (toLoadBalancer)
		{
			cluster_LoadBalancer = con;
			
			// Send server announce to load balancer immediately
			doActivity();
		}
		else
		{
			serverConnectionList.add(con);
		}
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
		if (!term && !serverConnectionList.remove(con))
		{
			clientConnectionList.remove(con);
		}
	}

	public boolean initiateSSLConnection(int port, String host, boolean toLoadBalancer)
	{
		// make a connection to another server if remote hostname is supplied
		if (host != null)
		{
			try
			{
				SSLSocketFactory ssf = (SSLSocketFactory) SSLSocketFactory.getDefault();
				final SSLSocket sslSocket = (SSLSocket) ssf.createSocket(host, port);
				
				// Enable all security suites
				String[] suites = sslSocket.getSupportedCipherSuites();
				sslSocket.setEnabledCipherSuites(suites);
				
				// Inform when hand shake completes
				sslSocket.addHandshakeCompletedListener(new HandshakeCompletedListener()
						{
							public void handshakeCompleted(HandshakeCompletedEvent e)
							{
								log.debug("Handshake succesful! Using cipher suite: " + e.getCipherSuite());
								
								// Retrieve and validate connected server's certificate chain
							    try
								{
									sslSocket.getSession().getPeerCertificates();
								}
								catch (SSLPeerUnverifiedException sslException)
								{
									log.debug("Server certificate verification failed: " + sslException.getMessage());
									
									try
									{
										sslSocket.close();
									}
									catch (IOException sslCloseException)
									{
										log.debug("SSLSocket close failed: " + sslCloseException.getMessage());
										
										interrupt();
									}
								}
							    
							    log.debug("Valid certificate!");
							}
						});
				
				// Will throw an IO exception if the server does not support SSL protocol
				try
				{
					sslSocket.startHandshake();
				}
				catch (IOException e1)
				{
					log.info("Server establish ssl connection failed. Target server does not support SSL protocol");
					
					try
					{
						sslSocket.close();
					}
					catch (IOException e)
					{
						log.debug("Server SSL socket close failed");

						return false;
					}
					
					return false;
				}
				
				outgoingConnection(sslSocket, toLoadBalancer);

				return true;
			}
			catch (UnknownHostException e)
			{
				log.info("Server establish ssl connection failed. Unknown Host: " + e.getMessage());

				System.exit(-1);
			}
			catch (IOException e)
			{
				log.error("failed to make ssl connection to " + host + ":"
						+ port + " :" + e);

				return false;
			}
		}

		return false;
	}

	public boolean initiateConnection(int port, String host, boolean toLoadBalancer)
	{
		// make a connection to another server if remote hostname is supplied
		if (host != null)
		{
			try
			{
				outgoingConnection(new Socket(host, port), toLoadBalancer);

				return true;
			}
			catch (UnknownHostException e)
			{
				log.info("Server establish ssl connection failed. Unknown Host: " + e.getMessage());

				System.exit(-1);
			}
			catch (IOException e)
			{
				log.error("Server failed to make plain connection to " + host + ":"
						+ port + " :" + e);

				return false;
			}
		}

		return false;
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
			
			return true;
		}
		
		if (!containCommandField(con, receivedJsonObj))
		{
			return true;
		}
		
		String msgType = receivedJsonObj.get("command").getAsString();

		switch (msgType)
		{
			case JsonMessage.CLIENT_AUTHENTICATE:
				return processClientAuthMsg(con, receivedJsonObj);
				
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
		cluster_LoadBalancer.writeMsg(serverAnnounceJsonStr);

		log.info("Server announcement sent");

		return false;
	}
	
	/*
	 * Other methods as needed
	 */
	private boolean processLogoutMsg(Connection con, JsonObject receivedJsonObj)
	{
		// Remove client from the client connection list
		/*Connection toBeDeleted = null;
		
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
		}*/
		
		log.info("user logout");

		clientConnectionList.remove(con);

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
		String clientSentId = receivedJsonObj.get("id").getAsString();

		if (!clientSentId.equals(id))
		{
			// Send login failed info
			log.info("server is too busy");
			
			InvalidMsg invalidMsg = new InvalidMsg();
			invalidMsg.setInfo("Client auth failed");
			
			con.writeMsg(invalidMsg.toJsonString());
			
			return true;
		}
		
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
	
	private boolean processClientAuthMsg(Connection con, JsonObject receivedJsonObj)
	{
		// Validate login message format
		if (!isUserInfoMsgValid(con, receivedJsonObj))
		{
			return true;
		}
		
		String username = receivedJsonObj.get("username").getAsString();
		String clientSentId = receivedJsonObj.get("id").getAsString();

		if (!clientSentId.equals(id))
		{
			// Send login failed info
			log.info("server is too busy");
			
			InvalidMsg invalidMsg = new InvalidMsg();
			invalidMsg.setInfo("Client auth faile");
			
			con.writeMsg(invalidMsg.toJsonString());
			
			return true;
		}
		
		log.info("Connected with broadcaster in as user " + username);

		LoginSuccMsg loginSuccMsg = new LoginSuccMsg();
		loginSuccMsg.setInfo("Connected with broadcaster successful");

		String loginSuccJsonStr = loginSuccMsg.toJsonString();
		con.writeMsg(loginSuccJsonStr);

		clientConnectionList.add(con);

		return false;
	}

	private boolean processAuthMsg(Connection con, JsonObject receivedJsonObj)
	{
		// This server has too many children
		if (serverConnectionList.size() >= SERVER_CONNECTION_UPPER_LIMIT)
		{
			log.info("Auth faield: too many servers connecting to this server");

			AuthFailMsg authFailedMsg = new AuthFailMsg();
			authFailedMsg.setInfo("Auth faield: too many servers connecting to this server");
			
			String authFailedJsonStr = authFailedMsg.toJsonString();
			con.writeMsg(authFailedJsonStr);

			return true;
		}
		// Json message format incorrect
		else if (!receivedJsonObj.has("secret"))
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

	public boolean connectToCluster()
	{
		return initiateSSLConnection(Settings.getRemotePort(), Settings.getRemoteHostname(), false) ||
			   initiateConnection(Settings.getRemotePort(), Settings.getRemoteHostname(), false);
	}

	public boolean connectToLoadBalancer()
	{
		return initiateSSLConnection(Settings.getLoadBalancerPort(), Settings.getLoadBalancerHostname(), true) ||
			   initiateConnection(Settings.getLoadBalancerPort(), Settings.getLoadBalancerHostname(), true);
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
}
