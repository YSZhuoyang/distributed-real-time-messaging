package activitystreamer.server;

import java.io.IOException;
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
	private static String sec = null;
	private static ArrayList<Connection> serverList = new ArrayList<Connection>();
	private static ArrayList<Connection> clientList = new ArrayList<Connection>();
	private static ArrayList<String> passwordList = new ArrayList<String>();
	private static int counter = 0;
	/*
	 * additional variables as needed
	 */

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

		/*
		 * Do some further initialization here if necessary
		 */

		// This is the root server
		if (Settings.getRemoteHostname() == null)
		{
			// generate secrete keys here
			sec = Settings.nextSecret();
			Settings.setSecret(sec);

			log.info("The secret key for this server is:" + sec.toString());
		}
		// else is not the root server;check if the secret is correct or the
		// command is valid
		else
		{
			// check if we should initiate a connection and do so if necessary
			initiateConnection();
		}

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
		Connection con = super.outgoingConnection(s);
		// con.writeMsg("hi, this is a new server yelling at you!!!");
		Gson gson = new Gson();
		AuthMsg AuthJson = new AuthMsg();
		// Set values
		AuthJson.setSecret(Settings.getSecret());
		String m = gson.toJson(AuthJson);
		con.writeMsg(m.toString());

		/*
		 * do additional things here
		 */
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
		
		switch(msgType)
		{
			case "LOGIN":
				// Client connect check load balance
				String secret = receivedJsonObj.get("secret").getAsString();
				
				if (passwordList.contains(secret))
				{
					LoginSuccMsg m = new LoginSuccMsg();
					m.setInfo("Login_Success");
					clientList.add(con);
					
					// Send login successful message
					
					log.info("Login_Success");
				}
				else
				{
					LoginFailed m = new LoginFailed();
					m.setInfo("Login_Failed");
					
					// Send faild info to client
					
				}

				break;
				
				case "REGISTER":
					// check load balance
					// RedirectMsg m = new RedirectMsg();

					// check lock
					passwordList.add(receivedJsonObj.get("secret").getAsString());
					RegistSuccMsg m = new RegistSuccMsg();
					m.setInfo("Register_Succ");
					String registSuccJsonStr = new Gson().toJson(m);
					
					log.info("Register_Succ");
					
					con.writeMsg(registSuccJsonStr);
					
					break;
					
				case "AUTHENTICATE":
					// Connect with server
					if (receivedJsonObj.get("secret").getAsString() != (Settings.getSecret()))
					{
						AuthFailMsg authJson = new AuthFailMsg();
						authJson.setInfo("Authetication failed");
						
						String message = new Gson().toJson(authJson);
						con.writeMsg(message);
					}
					
					break;
					
				case "AUTHENTICATE_FAILED":
					// print err msg
					
					break;
					
				case "LOGOUT":
					// Remove client from the client list
					
					return true;
					
				case "INVALID_MESSAGE":
					InvalidMsg invalidMsg = new InvalidMsg();
					invalidMsg.setInfo("");
					
					String message = new Gson().toJson(invalidMsg);
					con.writeMsg(message);
					
					break;
				case "ACTIVITY_MESSAGE":
					// send
					
					break;
					
				case "SERVER_ANNOUNCE":
					// ...
					
					break;
					
				case "ACTIVITY_BROADCAST":
					// broadcast
					
					break;
					
				case "LOCK_REQUEST":
					// ...
					
					break;
					
				case "LOCK_DENIED":
					// ...
					
					break;
					
				case "LOCK_ALLOWED":
					// ...
					
					break;
					
				default:
					break;
		}
		
		serverList.add(con);

		// if(serverList.size() > 0)
		// log.debug("This is from the 1st server that you connect!!");

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
		/*
		 * if(serverList.size()>0) serverList.get(0).writeMsg(
		 * "hi, this is the first msg root send to server 1.");
		 */
		return false;
	}

	/*
	 * Other methods as needed
	 */
	public void run()
	{
		/*
		 * try { ServerSocket listenSocket = new
		 * ServerSocket(Settings.getLocalPort());
		 * 
		 * while(true) { System.out.println("Server listening for a connection"
		 * ); Socket clientSocket = listenSocket.accept();
		 * 
		 * System.out.println("Received connection "); Connection c = new
		 * Connection(clientSocket); } } catch(IOException e) {
		 * System.out.println("Listen socket:"+e.getMessage()); }
		 */
	}
}
