package activitystreamer.server;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;

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
	private static ArrayList<Connection> serverList = new ArrayList<>();
	private ArrayList<Connection> clientList = new ArrayList<>();
	
	// All servers except itself
	private static ArrayList<ServerInfo> serverInfoList = new ArrayList<>();
	private ArrayList<String> clientSecretList = new ArrayList<>();

	// Assuming that id is secret
	//private String secret;
	//private String id;
	private int numClientConnections = 0;
	//private ServerInfo serverInfo;
	
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
		//id = Settings.nextSecret();

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
			serverInfo.setId(Settings.getSecret());
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
		
		AuthMsg AuthJson = new AuthMsg();
		AuthJson.setSecret(Settings.getSecret());
		String m = new Gson().toJson(AuthJson);
		serverList.add(con);
		
		con.writeMsg(m.toString());

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
		String secret;
		ServerInfo server;

		switch (msgType)
		{
			case JsonMessage.LOGIN:
				secret = receivedJsonObj.get("secret").getAsString();

				if (!clientSecretList.contains(secret))
				{
					LoginFailedMsg loginFailedMsg = new LoginFailedMsg();
					loginFailedMsg.setInfo("Register failed, secret does not exist!");
					String registFailedJsonStr = new Gson().toJson(loginFailedMsg);

					log.info("Register failed. Secret already exists!");

					con.writeMsg(registFailedJsonStr);

					return true;
				}
				
				LoginSuccMsg loginSuccMsg = new LoginSuccMsg();
				loginSuccMsg.setInfo("Login successful");
				String loginSuccJsonStr = new Gson().toJson(loginSuccMsg);

				log.info("Login_Success");

				con.writeMsg(loginSuccJsonStr);

				// Find a server with less client connection
				server = loadBalance();

				if (server == null)
				{
					numClientConnections++;

					break;
				}
				else
				{
					// Send lock request
					
					
					
					RedirectMsg redirectMsg = new RedirectMsg();
					redirectMsg.setHost(server.getRemoteHostname());
					redirectMsg.setPort("" + server.getRemotePort());
					String redirectMsgJsonStr = new Gson().toJson(redirectMsg);

					log.info("Redirected");

					con.writeMsg(redirectMsgJsonStr);

					return true;
				}

			case JsonMessage.REGISTER:
				secret = receivedJsonObj.get("secret").getAsString();

				// Check whether secret already exists
				if (clientSecretList.contains(secret))
				{
					RegisterFailedMsg registerFailedMsg = new RegisterFailedMsg();
					registerFailedMsg.setInfo("Register failed, secret already exists!");
					String registFailedJsonStr = new Gson().toJson(registerFailedMsg);

					log.info("Register failed. Secret already exists!");

					con.writeMsg(registFailedJsonStr);

					return true;
				}
				
				clientSecretList.add(secret);
				RegistSuccMsg registerSuccMsg = new RegistSuccMsg();
				registerSuccMsg.setInfo("Register_Succ");
				String registSuccJsonStr = new Gson().toJson(registerSuccMsg);
				con.writeMsg(registSuccJsonStr);
				numClientConnections++;

				log.info("Register_Success");
				break;

			case JsonMessage.AUTHENTICATE:
				secret = receivedJsonObj.get("secret").getAsString();
				
				// Connect with server
				if (!secret.equals(Settings.getSecret()))
				{
					log.info("Auth faield");
					
					AuthFailMsg authFailedMsg = new AuthFailMsg();
					authFailedMsg.setInfo("Authetication failed");
					String authFailedJsonStr = new Gson().toJson(authFailedMsg);
					con.writeMsg(authFailedJsonStr);
					
					return true;
				}
				else
				{
					log.info("Auth succeeded");
					
					serverList.add(con);
				}

				break;

			case JsonMessage.AUTHTENTICATION_FAIL:
				log.info("Authentication failed");

				System.exit(0);
				//return true;

			case JsonMessage.LOGOUT:
				// Remove client from the client connection list
				clientList.remove(con);

				log.info("user logout");

				return true;

			/*case "INVALID_MESSAGE":
				if (receivedJsonObj.get("command").getAsString() == "invalid_message")
					;
				InvalidMsg invalidMsg = new InvalidMsg();
				invalidMsg.setInfo("Invalid_Message");
				String invalidMessage = new Gson().toJson(invalidMsg);
				con.writeMsg(invalidMessage);

				break;
			*/
				
			case JsonMessage.ACTIVITY_MESSAGE:
				// send

				break;

			case JsonMessage.SERVER_ANNOUNCE:
				log.info("Server announce received");
				
				secret = receivedJsonObj.get("id").getAsString();
				ServerInfo serverInfo = findServer(secret);
				
				if (serverInfo == null)
				{
					serverInfo = new ServerInfo();
					serverInfo.setId(secret);
					serverInfo.setServerLoad(receivedJsonObj.get("load").getAsInt());
					serverInfo.setRemoteHostname(receivedJsonObj.get("host").getAsString());
					serverInfo.setRemotePort(receivedJsonObj.get("port").getAsInt());
					serverInfoList.add(serverInfo);
				}

				// Inform others
				ServerAnnounceMsg serverAnnounceMsg = new ServerAnnounceMsg();
				serverAnnounceMsg.setId(serverInfo.getId());
				serverAnnounceMsg.setHostname(serverInfo.getRemoteHostname());
				serverAnnounceMsg.setLoad(serverInfo.getServerLoad());
				serverAnnounceMsg.setPort(serverInfo.getRemotePort());
				
				String serverAnnounceJsonStr = new Gson().toJson(serverAnnounceMsg);
				
				log.debug("Connection number: " + serverList.size());
				
				for (Connection connection : serverList)
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

				break;

			case JsonMessage.ACTIVITY_BROADCAST:
				// broadcast

				break;

			case JsonMessage.LOCK_REQUEST:
				// ...

				break;

			case JsonMessage.LOCK_DENIED:
				// ...

				break;

			case JsonMessage.LOCK_ALLOWED:
				// ...

				break;

			default:
				break;
		}
		// if(serverList.size() > 0)
		// log.debug("This is from the 1st server that you connect!!");

		return false;

	}

	public ServerInfo findServer(String id)
	{
		//int index = -1;
		
		for (ServerInfo serverInfo : serverInfoList)
		{
			if (serverInfo.getId().equals(id))
			{
				return serverInfo;
			}
		}
		
		/*for (int i = 0; i < serverInfoList.size(); i++)
		{
			if (serverInfoList.get(i).getId().equals(id))
				index = i;
		}*/

		return null;
	}

	public void generateMsg(String hostname, String port)
	{
		RedirectMsg m = new RedirectMsg();
		m.setHost(hostname);
		m.setPort(port);
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
		serverAnnounceMsg.setId(Settings.getSecret());
		serverAnnounceMsg.setLoad(Settings.getNumClientConnections());
		serverAnnounceMsg.setPort(Settings.getLocalPort());
		
		String serverAnnounceJsonStr = new Gson().toJson(serverAnnounceMsg);
		
		for (Connection con : serverList)
		{
			con.writeMsg(serverAnnounceJsonStr);
		}
		
		log.info("Server announcement sent");
		
		return false;
	}
	
	/*public ServerInfo getServerInfo()
	{
		return serverInfo;
	}*/

	/*
	 * Other methods as needed
	 */
}
