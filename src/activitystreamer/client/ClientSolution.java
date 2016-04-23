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

import Message.JsonMessage;
import activitystreamer.util.Settings;

public class ClientSolution extends Thread
{
	private static final Logger log = LogManager.getLogger();
	private static ClientSolution clientSolution;
	private TextFrame textFrame;

	/*
	 * additional variables
	 */

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
		if (Settings.getRemoteHostname() != null)
		{
			try
			{
				Gson gson = new Gson();
				JsonMessage loginJson = new JsonMessage();
				
				// Set values
				loginJson.setSecret(Settings.getSecret());
				loginJson.setUsername(Settings.getUsername());
				
				String loginMessage = gson.toJson(loginJson);
				
				// Try to establish connection
				Socket socket = new Socket(Settings.getRemoteHostname(), Settings.getRemotePort());
				
				PrintWriter writer = new PrintWriter(new DataOutputStream(socket.getOutputStream()));
				writer.println(loginMessage);
				writer.flush();

				log.info("Message sent: " + loginMessage);
			}
			catch (IOException e)
			{
				e.printStackTrace();
			}
			
			//PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
			
			
			
			// Send a json obj including client info to a remote server
			/*try
			{
				Gson gson = new Gson();
			}
			catch (IOException e)
			{
				log.error("failed to make connection to " + Settings.getRemoteHostname() + ":"
						+ Settings.getRemotePort() + " :" + e);
				System.exit(-1);
			}*/
			
			//log.info("The secret key for this client is:" + sec.toString());
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
		
	}

	/*
	 * additional methods
	 */

}
