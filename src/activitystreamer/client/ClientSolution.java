package activitystreamer.client;

import java.io.IOException;
import java.net.Socket;
import java.net.UnknownHostException;

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
import activitystreamer.util.Settings;


public class ClientSolution extends Thread
{
	private static final Logger log = LogManager.getLogger();
	private static ClientSolution clientSolution;
	
	private ClientConnection connection;
	private String serverId;
	
	private TextFrame textFrame;
	private MainFrame mainFrame;
	
	/*
	 * additional variables
	 */
	private boolean connectionClosed;
	
	// this is a singleton object
	public static synchronized ClientSolution getInstance()
	{
		if (clientSolution == null)
		{
			clientSolution = new ClientSolution();
		}
		
		return clientSolution;
	}

	private ClientSolution()
	{
		// Tell the client to trust the server certificate
		System.setProperty("javax.net.ssl.trustStore", "./Certificate/mykey");
		
		connectionClosed = true;
		
		start();
	}

	// called by the gui when the user clicks "send"
	public void sendActivityObject(String activityContent)
	{
		log.info("Activity message sent: " + activityContent);
		
		ActivityMsg activityMsg = new ActivityMsg();
		activityMsg.setUsername(Settings.getUsername());
		activityMsg.setSecret(Settings.getSecret());
		activityMsg.setObject(activityContent);
		activityMsg.setId(serverId);
		
		String activityMessage = activityMsg.toJsonString();
		connection.writeMsg(activityMessage);
	}
	
	// called by the gui when the user clicks disconnect
	public void disconnect()
	{
		/*
		 * other things to do
		 */
		sendLogoutMsg();
		closeConnection();
		mainFrame.close();
		textFrame.dispose();
	}
	
	public synchronized boolean process(String receivedJsonStr)
	{
		log.debug("Client received: " + receivedJsonStr);
		
		JsonObject receivedJson;
		
		try
		{
			receivedJson = new Gson().fromJson(receivedJsonStr, JsonObject.class);
		}
		catch (JsonSyntaxException e)
		{
			log.debug("Client receiving msg failed. Not json format: " + e.getMessage());
			
			return true;
		}
		
		if (!containCommandField(receivedJson))
		{
			return true;
		}
		
		String command = receivedJson.get("command").getAsString();
		
		switch (command)
		{
			case JsonMessage.ACTIVITY_BROADCAST:
				log.info("Activity broadcast received");
				
				textFrame.displayActivityMessageText(receivedJson);
				
				return false;
			
			case JsonMessage.REGISTER_SUCCESS:
				return processRegisterSuccessMsg(receivedJson);
				
			case JsonMessage.REGISTER_FAILED:
				return processRegisterFailedMsg(receivedJson);
				
			case JsonMessage.REDIRECT:
				return processRedirectMsg(receivedJson);
			
			case JsonMessage.AUTHENTICATION_FAIL:
				log.info("Client failed to send activity message to server.");
				
				// Close the current connection
				disconnect();
				
				return true;

			case JsonMessage.LOGIN_SUCCESS:
				return processLoginSuccessMsg();
				
			case JsonMessage.LOGIN_FAILED:
				return processLoginFailedMsg(receivedJson);
				
			case JsonMessage.INVALID_MESSAGE:
				return processInvalidMsg(receivedJson);
			
			default:
				return processUnknownMsg(receivedJson);
		}
	}

	// the client's run method, to receive messages
	@Override
	public void run()
	{
		log.debug("Client started");
		
		// Do something!!!
		mainFrame = new MainFrame();
	}
	
	/*
	 * additional methods
	 */
	private boolean processLoginFailedMsg(JsonObject receivedJson)
	{
		log.info("Login failed");
		
		String loginFaildInfo = receivedJson.get("info").getAsString();
		mainFrame.showInfoBox(loginFaildInfo);
		closeConnection();
		
		return true;
	}
	
	private boolean processLoginSuccessMsg()
	{
		log.info("Login success received");

		// open the gui
		log.debug("opening the gui");
		
		mainFrame.hide();
		
		if (textFrame == null)
		{
			textFrame = new TextFrame();
		}
		
		return false;
	}
	
	private boolean processRegisterSuccessMsg(JsonObject receivedJsonObj)
	{
		log.info("Register success received");
		
		String info = receivedJsonObj.get("info").getAsString();
		mainFrame.showInfoBox(info);
		closeConnection();
		
		return true;
	}
	
	private boolean processUnknownMsg(JsonObject receivedJsonObj)
	{
		log.info("Unknown message received");
		
		disconnect();
		
		return true;
	}
	
	private boolean processRedirectMsg(JsonObject receivedJsonObj)
	{
		log.info("Redirect");
		
		// Close the current connection
		closeConnection();
		
		// Setup with new host and port number
		serverId = receivedJsonObj.get("id").getAsString();
		String newHost = receivedJsonObj.get("hostname").getAsString();
		int newPort = receivedJsonObj.get("port").getAsInt();
		
		Settings.setRemoteHostname(newHost);
		Settings.setRemotePort(newPort);
		
		// Reconnect to another server
		log.info("Connect to another server");
		
		if (connect())
		{
			sendClientAuthMsg();
			
			return false;
		}
		
		return true;
	}
	
	private boolean processInvalidMsg(JsonObject receivedJsonObj)
	{
		log.info("Client failed to send activity message to server.");
		
		String info = receivedJsonObj.get("info").getAsString();
		textFrame.showErrorMsg(info);
		disconnect();
		
		return true;
	}
	
	private boolean processRegisterFailedMsg(JsonObject receivedJsonObj)
	{
		log.info("Register failed");
		
		String info = receivedJsonObj.get("info").getAsString();
		
		mainFrame.showInfoBox(info);
		closeConnection();
		
		return true;
	}
	
	private boolean containCommandField(JsonObject receivedJsonObj)
	{
		if (!receivedJsonObj.has("command"))
		{
			InvalidMsg invalidMsg = new InvalidMsg();
			invalidMsg.setInfo("Message must contain field command");
			
			connection.writeMsg(invalidMsg.toJsonString());
			
			return false;
		}
		
		return true;
	}
	
	public synchronized boolean establishConnection()
	{
		try
		{
			Socket socket = new Socket(Settings.getRemoteHostname(), Settings.getRemotePort());
			connection = new ClientConnection(socket);
			connectionClosed = false;
			
			return true;
		}
		catch (UnknownHostException e1)
		{
			log.info("Client establish connection failed. Unknown Host: " + e1.getMessage());
			//e1.printStackTrace();
			
			return false;
		}
		catch (IOException e)
		{
			log.debug("Client establish connection failed. IO reader init failed: " + e.getMessage());
			
			return false;
		}
	}
	
	public synchronized boolean establishSSLConnection()
	{
		SSLSocketFactory ssf = (SSLSocketFactory) SSLSocketFactory.getDefault();
		final SSLSocket sslSocket;
		
		try
		{
			sslSocket = (SSLSocket) ssf.createSocket(Settings.getRemoteHostname(), Settings.getRemotePort());
		}
		catch (UnknownHostException e)
		{
			log.info("Client establish ssl connection failed. Unknown Host: " + e.getMessage());
			//e.printStackTrace();
			
			return false;
		}
		catch (IOException e)
		{
			log.debug("Client establish ssl connection failed. IO exception: " + e.getMessage());
			//e.printStackTrace();
			
			return false;
		}
		
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
		
		// Throw an IO exception if the server does not support SSL protocol
		try
		{
			sslSocket.startHandshake();
		}
		catch (IOException e)
		{
			log.info("Client establish connection failed. Server does not support SSL protocol: " + e.getMessage());
			
			try
			{
				sslSocket.close();
			}
			catch (IOException ioException)
			{
				log.debug("Client SSL socket close failed: " + ioException.getMessage());

				return false;
			}
			
			return false;
		}
		
		try
		{
			connection = new ClientConnection(sslSocket);
		}
		catch (IOException e1)
		{
			log.info("Client establishes ssl connection failed. Init failed: " + e1.getMessage());
			
			return false;
		}
		
		connectionClosed = false;
		
		return true;
	}
	
	public synchronized boolean connect()
	{
		return establishSSLConnection() || establishConnection();
	}
	
	private synchronized void closeConnection()
	{
		log.debug("Client connection closed");
		
		if (!connectionClosed)
		{
			connection.closeCon();
			
			connectionClosed = true;
		}
	}
	
	public void sendRegisterMsg()
	{
		if (connectionClosed)
		{
			return;
		}
		
		RegisterMsg registerMsg = new RegisterMsg();
		registerMsg.setUsername(Settings.getUsername());
		registerMsg.setSecret(Settings.getSecret());
		String registerMessage = registerMsg.toJsonString();
		
		connection.writeMsg(registerMessage);
	}
	
	public void sendAnonymusLoginMsg()
	{
		if (connectionClosed)
		{
			return;
		}
		
		AnonymousLoginMsg anonymusLoginMsg = new AnonymousLoginMsg();
		String anonymusLoginMessage = anonymusLoginMsg.toJsonString();
		
		Settings.setUsername(JsonMessage.ANONYMOUS_USERNAME);
		Settings.setSecret("");
		
		connection.writeMsg(anonymusLoginMessage);
	}
	
	public void sendLoginMsg()
	{
		if (connectionClosed)
		{
			return;
		}
		
		LoginMsg loginMsg = new LoginMsg();
		loginMsg.setUsername(Settings.getUsername());
		loginMsg.setSecret(Settings.getSecret());
		String loginMessage = loginMsg.toJsonString();
		
		connection.writeMsg(loginMessage);
	}
	
	private void sendClientAuthMsg()
	{
		if (connectionClosed)
		{
			return;
		}
		
		ClientAuthenticateMsg clientAuthMsg = new ClientAuthenticateMsg();
		clientAuthMsg.setUsername(Settings.getUsername());
		clientAuthMsg.setSecret(Settings.getSecret());
		clientAuthMsg.setId(serverId);
		
		connection.writeMsg(clientAuthMsg.toJsonString());
	}
	
	public void sendLogoutMsg()
	{
		if (connectionClosed)
		{
			return;
		}
		
		LogoutMsg logoutMsg = new LogoutMsg();
		logoutMsg.setUsername(Settings.getUsername());
		logoutMsg.setSecret(Settings.getSecret());
		
		connection.writeMsg(logoutMsg.toJsonString());
	}
}
