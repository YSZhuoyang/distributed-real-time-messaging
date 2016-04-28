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
import org.json.simple.JSONObject;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.ibm.common.activitystreams.Activity;

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
		
		// Test: register only
		if (Settings.getRemoteHostname() != null)
		{
			// Connect to a server host
			establishConnection();
			
			// Testing
			sendRegisterMsg();
			
			// open the gui
			log.debug("opening the gui");
			
			textFrame = new TextFrame();
			
			// start the client's thread
			start();
		}
		else
		{
			log.debug("Host name is empty");
		}
	}

	// called by the gui when the user clicks "send"
	public void sendActivityObject(JSONObject activityObj)
	{
		try
		{
			if (activityObj.get("command").toString().equals("ACTIVITY_MESSAGE")){
			ActivityMsg activityMsg = new ActivityMsg();
			activityMsg.setUsername(activityObj.get("username").toString());
			activityMsg.setSecret(activityObj.get("secret").toString());
			activityMsg.setUserActivity((Activity)activityObj.get("activity"));
			String activityMessage = activityMsg.toJsonString();

			// Try to establish connection
			socket = new Socket(Settings.getRemoteHostname(), Settings.getRemotePort());
			writer = new PrintWriter(socket.getOutputStream(), true);
			writer.println(activityMessage);
			}
		}
		catch (IOException e)
		{
			e.printStackTrace();
		}
		
		
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
						
						closeConnection();
						
						System.exit(0);
						
					case JsonMessage.REDIRECT:
						log.info("Redirect");
						
						// Close the current connection
						closeConnection();
						
						// Setup with new host and port number
						String newHost = receivedJson.get("host").getAsString();
						int newPort = receivedJson.get("port").getAsInt();

						Settings.setRemoteHostname(newHost);
						Settings.setRemotePort(newPort);

						// Reconnect to another server
						log.info("Connect to another server");
						
						establishConnection();
						sendLoginMsg();
						
						break;
						
					case JsonMessage.LOGIN_FAILED:
						log.info("Login failed");
						
						closeConnection();
						
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
	private void establishConnection()
	{
		try
		{
			socket = new Socket(Settings.getRemoteHostname(), Settings.getRemotePort());
			
			in = new DataInputStream(socket.getInputStream());
			inreader = new BufferedReader(new InputStreamReader(in));
			
			out = new DataOutputStream(socket.getOutputStream());
			writer = new PrintWriter(out, true);
			
		}
		catch (IOException e)
		{
			log.debug("Client establish connection failed: " + e.getMessage());
		}
	}
	
	private void closeConnection()
	{
		try
		{
			in.close();
			inreader.close();
			
			out.close();
			writer.close();
		}
		catch (IOException e)
		{
			log.debug("Client close connection failed: " + e.getMessage());
		}
	}

	private void sendRegisterMsg()
	{
		RegisterMsg registerMsg = new RegisterMsg();
		registerMsg.setUsername(Settings.getUsername());
		registerMsg.setSecret(Settings.getSecret());
		String registerMessage = registerMsg.toJsonString();

		writer.println(registerMessage);
	}
	
	private void sendLoginMsg()
	{
		LoginMsg loginMsg = new LoginMsg();
		loginMsg.setUsername(Settings.getUsername());
		loginMsg.setSecret(Settings.getSecret());
		String loginMessage = loginMsg.toJsonString();

		writer.println(loginMessage);
	}
}
