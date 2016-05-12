package activitystreamer.server.LoadBalancer;

import java.io.IOException;
import java.net.Socket;

import activitystreamer.util.Listener;


public class LoadBalancerListener extends Listener
{
	
	protected static synchronized LoadBalancerListener getInstance()
	{
		if (listener == null)
		{
			try
			{
				listener = new LoadBalancerListener();
			}
			catch (IOException e)
			{
				log.debug("LoadBalancerListener init failed: " + e.getMessage());
			}
		}

		return (LoadBalancerListener) listener;
	}
	
	private LoadBalancerListener() throws IOException
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
				LoadBalancerSolution.getInstance().incomingConnection(clientSocket);
			}
			catch (IOException e)
			{
				log.info("received exception, shutting down");
				
				term = true;
			}
		}
	}

	public void setTerm(boolean term)
	{
		this.term = term;
		
		if (term)
		{
			try
			{
				sslServerSocket.close();
			}
			catch (IOException io)
			{
				log.error("Server socket closed error");
			}
		}
	}
}
