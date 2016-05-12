package activitystreamer.util;

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;


public class Connection extends Thread
{
	protected static final Logger log = LogManager.getLogger();
	
	private DataInputStream in;
	private DataOutputStream out;
	private PrintWriter outwriter;
	protected BufferedReader inreader;
	
	private boolean open = false;
	protected boolean term = false;

	protected Socket socket = null;
	
	public Connection(Socket socket) throws IOException
	{
		in = new DataInputStream(socket.getInputStream());
		out = new DataOutputStream(socket.getOutputStream());
		inreader = new BufferedReader(new InputStreamReader(in));
		outwriter = new PrintWriter(out, true);
		
		this.socket = socket;
		open = true;
		
		start();
	}
	
	/*
	 * returns true if the message was written, otherwise false
	 */
	public boolean writeMsg(String msg)
	{
		if (open)
		{
			outwriter.println(msg);
			
			return true;
		}
		
		return false;
	}

	public void closeCon()
	{
		if (open)
		{
			log.info("closing connection " + Settings.socketAddress(socket));
			
			term = true;
		}
	}
	
	protected void closeStream()
	{
		// Close streams and readers
		try
		{
			inreader.close();
			in.close();
			
			outwriter.close();
			out.close();
		}
		catch (IOException e)
		{
			// already closed?
			log.error("received exception closing the connection " + Settings.socketAddress(socket) + ": " + e);
		}
		
		open = false;
	}

	public void run()
	{
		
	}
	
	public Socket getSocket()
	{
		return socket;
	}

	public boolean isOpen()
	{
		return open;
	}
}
