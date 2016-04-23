package activitystreamer.server;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.ArrayList;







import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import com.google.gson.Gson;
import com.google.gson.JsonObject;

import Message.AuthFailMsg;
import Message.AuthMsg;
import Message.InvalidMsg;
import Message.LoginFailed;
import Message.LoginMsg;
import Message.LoginSuccMsg;
import Message.LogoutMsg;
import Message.RedirectMsg;
import Message.RegistSuccMsg;
import activitystreamer.util.Settings;

public class ControlSolution extends Control
{
	private static final Logger log = LogManager.getLogger();
	private static String sec = null;
	private static ArrayList<Connection> serverList = new ArrayList<Connection>();
	private static ArrayList<Connection> clientList = new ArrayList<Connection>();
	private static ArrayList<String> passwordList = new ArrayList<String>();
	private static int counter=0;
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

		//This is the root server
		if(Settings.getRemoteHostname() == null)
		{
			//generate secrete keys here
			sec = Settings.nextSecret();
			Settings.setSecret(sec);
			
			log.info("The secret key for this server is:" + sec.toString());
		}
		// else is not the root server;check if the secret is correct or the command is valid
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
		//con.writeMsg("hi, this is a new server yelling at you!!!");
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
		JSONObject obj = null;
		Gson gson = new Gson();
		String msgType = null;
		/*
		 * do additional work here return true/false as appropriate
		 */
		//log.debug(msg);
		log.info("Receieved: " + msg);
		try{
			JSONParser parser = new JSONParser();
			try
			{
				obj = (JSONObject) parser.parse(msg);
			}
			catch (ParseException e)
			{
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			msgType = (String) obj.get("command");
			// Client connect check load balance
			if (msgType.equals("LOGIN"))
			{	
				String password = obj.get("secret").toString();
				if(passwordList.contains(password)){
					LoginSuccMsg m = new LoginSuccMsg();
					m.setInfo("Login_Success");
					log.info("Login_Success");
					
				}
				else{
					LoginFailed m = new LoginFailed();
					m.setInfo("Login_Failed");
					
				}
				
				// check load balance
			}
			else if (msgType.equals("LOGIN_SUCCESS")){
				clientList.add(con);
			}
			else if (msgType.equals("LOGIN_FAILED")){
				
			}
			else if (msgType.equals("REGISTER"))
			{
				// check load balance
				//RedirectMsg m = new RedirectMsg();
				
				// check lock
				passwordList.add(obj.get("secret").toString());
				RegistSuccMsg m = new RegistSuccMsg();
				m.setInfo("Register_Succ");
				log.info("Register_Succ");
			}
			else if (msgType.equals("REGISTER_SUCCESS")){
				
			}
			else if (msgType.equals("REGISTER_FAILED")){
				
			}
			else if (msgType.equals("AUTHENTICATE"))
			{
				// Connect with server
				if( (String) obj.get("secret") != (Settings.getSecret())){
					AuthFailMsg AuthJson = new AuthFailMsg();
					// Set values
					AuthJson.setInfo("Authetication_Fail");
					String m = gson.toJson(AuthJson);
					con.writeMsg(m);
				}
				
			}
			else if (msgType.equals("AUTHENTICATE_FAILED")){
				
			}
			else if (msgType.equals("LOGOUT"))
			{
				// Remove client from the client list
				LogoutMsg m = new LogoutMsg();
				return true;
			}
			else if (msgType.equals("INVALID_MESSAGE")){
				InvalidMsg m = new InvalidMsg();
				m.setInfo("");
			}
			else if (msgType.equals("REDIRECT")){
				
			}
			else if (msgType.equals("ACTIVITY_MESSAGE")){
				
			}
			else if (msgType.equals("SERVER_ANNOUNCE")){
				
			}
			else if (msgType.equals("ACTIVITY_BROADCAST")){
				
			}
			else if (msgType.equals("LOCK_REQUEST")){
				
			}
			else if (msgType.equals("LOCK_DENIED")){
				
			}
			else if (msgType.equals("LOCK_ALLOWED")){
				
			}
		}
		catch (NullPointerException e){e.printStackTrace();}
		serverList.add(con);
		
		//if(serverList.size() > 0)
		//	log.debug("This is from the 1st server that you connect!!");
		
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
		/*if(serverList.size()>0)
			serverList.get(0).writeMsg("hi, this is the first msg root send to server 1.");
*/
		return false;
	}

	/*
	 * Other methods as needed
	 */
	public void run()
	{
		/*try
		{
			ServerSocket listenSocket = new ServerSocket(Settings.getLocalPort());
			
			while(true)
			{
				System.out.println("Server listening for a connection");
				Socket clientSocket = listenSocket.accept();

				System.out.println("Received connection ");
				Connection c = new Connection(clientSocket);
			}
		}
		catch(IOException e)
		{
			System.out.println("Listen socket:"+e.getMessage());
		}*/
	}
}
