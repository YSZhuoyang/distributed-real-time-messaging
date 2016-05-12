package activitystreamer.server.Broadcaster;

import java.io.IOException;
import java.net.Socket;

import activitystreamer.util.Connection;
import activitystreamer.util.Settings;


public class ClusterConnection extends Connection
{
	
	public ClusterConnection(Socket socket) throws IOException
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
				term = BroadcasterSolution.getInstance().process(this, data);
			}
			
			log.debug("connection closed to " + Settings.socketAddress(socket));
		}
		catch (IOException e)
		{
			log.error("connection " + Settings.socketAddress(socket) + " closed with exception: " + e);
			
			BroadcasterSolution.getInstance().connectionClosed(this);
		}

		BroadcasterSolution.getInstance().connectionClosed(this);
		closeStream();
	}
}
