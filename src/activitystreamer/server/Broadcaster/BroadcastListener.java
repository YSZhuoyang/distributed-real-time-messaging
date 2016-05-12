package activitystreamer.server.Broadcaster;

import java.io.IOException;
import java.net.Socket;

import activitystreamer.util.Listener;


public class BroadcastListener extends Listener
{
	
	protected static synchronized BroadcastListener getInstance()
	{
		if (listener == null)
		{
			try
			{
				listener = new BroadcastListener();
			}
			catch (IOException e)
			{
				log.debug("BroadcastListener init failed: " + e.getMessage());
			}
		}

		return (BroadcastListener) listener;
	}
	
	private BroadcastListener() throws IOException
	{
		super();
	}

	@Override
	public void run()
	{
		log.info("listening for new connections on " + portnum);
		
		while (!term)
		{
			try
			{
				Socket clientSocket = sslServerSocket.accept();
				BroadcasterSolution.getInstance().incomingConnection(clientSocket);
			}
			catch (IOException e)
			{
				log.info("received exception, shutting down");
				
				term = true;
			}
		}
	}
}
