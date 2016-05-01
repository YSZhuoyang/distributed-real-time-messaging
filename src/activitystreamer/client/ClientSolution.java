package activitystreamer.client;

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.google.gson.Gson;
import com.google.gson.JsonObject;

import Message.*;
import activitystreamer.util.Settings;


public class ClientSolution extends Thread
{
	private static final Logger log = LogManager.getLogger();
	private static ClientSolution clientSolution;
	private TextFrame textFrame;
	
	/*
	 * additional variables
	 */
	private Socket socket;
	private PrintWriter writer;
	private DataInputStream in;
	private DataOutputStream out;
	private BufferedReader inreader;
	private boolean connectionClosed;
	private MainFrame mainFrame = null;
	
	// this is a singleton object
	public static ClientSolution getInstance()
	{
		if (clientSolution == null)
		{
			clientSolution = new ClientSolution();
		}
		
		return clientSolution;
	}

	public ClientSolution()
	{
		/*
		 * some additional initialization
		 */
		connectionClosed = true;
	}

	// called by the gui when the user clicks "send"
	public void sendActivityObject(String activityContent)
	{
		log.info("Activity message sent: " + activityContent);
		
		ActivityMsg activityMsg = new ActivityMsg();
		activityMsg.setUsername(Settings.getUsername());
		activityMsg.setSecret(Settings.getSecret());
		activityMsg.setObject(activityContent);
		
		String activityMessage = activityMsg.toJsonString();
		writer.println(activityMessage);
	}
	
	// called by the gui when the user clicks disconnect
	public void disconnect()
	{
		textFrame.setVisible(false);

		/*
		 * other things to do
		 */
		sendLogoutMsg();
		closeConnection();
		mainFrame.close();
	}
	
	public void attachMainFrame(MainFrame mainFrame)
	{
		this.mainFrame = mainFrame;
	}
	
	private synchronized boolean process(String receivedJsonStr)
	{
		log.debug("Client received: " + receivedJsonStr);
		
		try
		{
			JsonObject receivedJson = new Gson().fromJson(receivedJsonStr, JsonObject.class);
			
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
					log.info("Login failed");
					
					disconnect();
					
					return true;
					
				case JsonMessage.INVALID_MESSAGE:
					log.info("Client failed to send activity message to server.");
					
					return processInvalidMsg(receivedJson);
				
				default:
					return processUnknownMsg(receivedJson);
			}
		}
		catch (Exception e)
		{
			log.debug("Client receiving message failed: " + e.getMessage());
			
			return true;
		}
	}

	// the client's run method, to receive messages
	@Override
	public void run()
	{
		log.debug("Client started");
		
		try
		{
			while (!connectionClosed)
			{
				String receivedMsg = inreader.readLine();
				
				connectionClosed = process(receivedMsg);
			}
		}
		catch (IOException e)
		{
			interrupt();
		}
	}
	
	/*
	 * additional methods
	 */
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
		mainFrame.close();
		
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
		String newHost = receivedJsonObj.get("hostname").getAsString();
		int newPort = receivedJsonObj.get("port").getAsInt();
		
		Settings.setRemoteHostname(newHost);
		Settings.setRemotePort(newPort);
		
		// Reconnect to another server
		log.info("Connect to another server");
		
		establishConnection();
		sendLoginMsg();
		
		return false;
	}
	
	private boolean processInvalidMsg(JsonObject receivedJsonObj)
	{
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
		mainFrame.close();
		
		return true;
	}
	
	private boolean containCommandField(JsonObject receivedJsonObj)
	{
		if (!receivedJsonObj.has("command"))
		{
			InvalidMsg invalidMsg = new InvalidMsg();
			invalidMsg.setInfo("Message must contain field command");
			writer.println(invalidMsg.toJsonString());
			
			return false;
		}
		
		return true;
	}
	
	public synchronized void establishConnection()
	{
		try
		{
			socket = new Socket(Settings.getRemoteHostname(), Settings.getRemotePort());
			
			in = new DataInputStream(socket.getInputStream());
			inreader = new BufferedReader(new InputStreamReader(in));
			
			out = new DataOutputStream(socket.getOutputStream());
			writer = new PrintWriter(out, true);
			
			connectionClosed = false;	
		}
		catch (IOException e)
		{
			log.debug("Client establish connection failed: " + e.getMessage());
		}
	}
	
	private synchronized void closeConnection()
	{
		try
		{
			in.close();
			inreader.close();
			
			out.close();
			writer.close();
			
			//socket.close();
			
			connectionClosed = true;
		}
		catch (IOException e)
		{
			log.debug("Client close connection failed: " + e.getMessage());
		}
	}

	public void sendRegisterMsg()
	{
		RegisterMsg registerMsg = new RegisterMsg();
		registerMsg.setUsername(Settings.getUsername());
		registerMsg.setSecret(Settings.getSecret());
		String registerMessage = registerMsg.toJsonString();

		writer.println(registerMessage);
	}
	
	public void sendAnonymusLoginMsg()
	{
		AnonymousLoginMsg anonymusLoginMsg = new AnonymousLoginMsg();
		String anonymusLoginMessage = anonymusLoginMsg.toJsonString();
		
		Settings.setUsername(JsonMessage.ANONYMOUS_USERNAME);
		Settings.setSecret("");

		writer.println(anonymusLoginMessage);
	}
	
	public void sendLoginMsg()
	{
		LoginMsg loginMsg = new LoginMsg();
		loginMsg.setUsername(Settings.getUsername());
		loginMsg.setSecret(Settings.getSecret());
		String loginMessage = loginMsg.toJsonString();

		writer.println(loginMessage);
	}
	
	public void sendLogoutMsg()
	{
		LogoutMsg logoutMsg = new LogoutMsg();
		logoutMsg.setUsername(Settings.getUsername());
		logoutMsg.setSecret(Settings.getSecret());
		
		writer.println(logoutMsg.toJsonString());
		
		connectionClosed = true;
	}
}
