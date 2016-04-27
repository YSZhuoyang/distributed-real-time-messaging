package activitystreamer.client;

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.json.simple.JSONObject;
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
		
		// Connect to a server host
		
		// Test: register only
		if (Settings.getRemoteHostname() != null)
		{
			sendRegisterMsg();
		}
		
		// open the gui
		log.debug("opening the gui");
		textFrame = new TextFrame();
		
		// start the client's thread
		start();
	}

	// called by the gui when the user clicks "send"
	public void sendActivityObject(JSONObject activityObj)
	{
		
	}

	// called by the gui when the user clicks disconnect
	public void disconnect()
	{
		textFrame.setVisible(false);
		/*
		 * other things to do
		 */
	}

	// the client's run method, to receive messages
	@Override
	public void run()
	{
		String receivedMsg;

		log.debug("Client started");
		
		try
		{
			DataInputStream in = new DataInputStream(socket.getInputStream());
			BufferedReader inreader = new BufferedReader(new InputStreamReader(in));
			
			while (true)
			{
				receivedMsg = inreader.readLine();
				
				log.debug("Client received: " + receivedMsg);
				
				JsonObject receivedJson = new Gson().fromJson(receivedMsg, JsonObject.class);
				String state = receivedJson.get("command").getAsString();
				
				switch (state)
				{
					case JsonMessage.REGISTER_FAILED:
						log.info("Register failed");
						System.exit(0);
						
					case JsonMessage.REDIRECT:
						log.info("Redirect");
						
						//socket.close();
						//writer.close();
						
						// reconnect
						log.info("Connect to another server");
						
						String newHost = receivedJson.get("host").getAsString();
						int newPort = receivedJson.get("port").getAsInt();
						
						Settings.setRemoteHostname(newHost);
						Settings.setRemotePort(newPort);

						sendLoginMsg();
						
						break;
						
					case JsonMessage.LOGIN_FAILED:
						log.info("Login failed");
						System.exit(0);
					
					default:
						break;
				}
				
			}
		}
		catch (IOException e)
		{
			System.err.println("Client receiving msg failed: " + e.getMessage());
		}
	}

	/*
	 * additional methods
	 */
	private void sendRegisterMsg()
	{
		try
		{
			RegisterMsg registerMsg = new RegisterMsg();
			registerMsg.setUsername(Settings.getUsername());
			registerMsg.setSecret(Settings.getSecret());
			String registerMessage = registerMsg.toJsonString();

			// Try to establish connection
			socket = new Socket(Settings.getRemoteHostname(), Settings.getRemotePort());
			writer = new PrintWriter(socket.getOutputStream(), true);
			writer.println(registerMessage);
		}
		catch (IOException e)
		{
			e.printStackTrace();
		}
	}
	
	private void sendLoginMsg()
	{
		try
		{
			LoginMsg loginMsg = new LoginMsg();
			loginMsg.setUsername(Settings.getUsername());
			loginMsg.setSecret(Settings.getSecret());
			String loginMessage = loginMsg.toJsonString();

			// Try to establish connection
			socket = new Socket(Settings.getRemoteHostname(), Settings.getRemotePort());
			writer = new PrintWriter(socket.getOutputStream(), true);
			writer.println(loginMessage);
		}
		catch (IOException e)
		{
			e.printStackTrace();
		}
	}
}
