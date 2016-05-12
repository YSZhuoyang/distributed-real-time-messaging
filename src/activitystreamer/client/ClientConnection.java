package activitystreamer.client;

import java.io.IOException;
import java.net.Socket;

import activitystreamer.util.Connection;
import activitystreamer.util.Settings;


public class ClientConnection extends Connection
{
	
	public ClientConnection(Socket socket) throws IOException
	{
		super(socket);
	}
	
	public void run()
	{
		log.info("connection running");
		
		String data;
		
		try
		{
			while (!term && (data = inreader.readLine()) != null)
			{
				term = ClientSolution.getInstance().process(data);
			}
			
			log.debug("connection closed to " + Settings.socketAddress(socket));
		}
		catch (IOException e)
		{
			log.error("connection " + Settings.socketAddress(socket) + " closed with exception: " + e);
		}
		
		closeStream();
	}
}
