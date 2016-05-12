package activitystreamer.util;

import java.io.IOException;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.ArrayList;

import javax.net.ssl.HandshakeCompletedEvent;
import javax.net.ssl.HandshakeCompletedListener;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import activitystreamer.util.Listener;


public class Control extends Thread
{
	protected static final Logger log = LogManager.getLogger();

	private static ArrayList<Connection> connections;

	protected static Control control;
	protected static Listener listener;
	protected static boolean term = false;
	
	protected Control()
	{
		// Tell the server to trust the certificate when it receives is from other servers
		System.setProperty("javax.net.ssl.trustStore", "./Certificate/mykey");
		
		// initialize the connections array
		connections = new ArrayList<Connection>();
	}
	
	public boolean initiateSSLConnection(int port, String host, boolean toLoadBalancer)
	{
		// make a connection to another server if remote hostname is supplied
		if (host != null)
		{
			try
			{
				SSLSocketFactory ssf = (SSLSocketFactory) SSLSocketFactory.getDefault();
				SSLSocket sslSocket = (SSLSocket) ssf.createSocket(host, port);
				
				// Enable all security suites
				String[] suites = sslSocket.getSupportedCipherSuites();
				sslSocket.setEnabledCipherSuites(suites);
				
				// Inform when hand shake completes
				sslSocket.addHandshakeCompletedListener(new HandshakeCompletedListener()
						{
							public void handshakeCompleted(HandshakeCompletedEvent e)
							{
								log.debug("Handshake succesful! Using cipher suite: " + e.getCipherSuite());
							}
						});
				
				// Will throw an IO exception if the server does not support SSL protocol
				try
				{
					sslSocket.startHandshake();
				}
				catch (IOException e1)
				{
					log.info("Server establish ssl connection failed. Target server does not support SSL protocol");
					
					try
					{
						sslSocket.close();
					}
					catch (IOException e)
					{
						log.debug("Server SSL socket close failed");

						return false;
					}
					
					return false;
				}
				
				outgoingConnection(sslSocket);

				return true;
			}
			catch (UnknownHostException e)
			{
				log.info("Server establish ssl connection failed. Unknown Host: " + e.getMessage());

				System.exit(-1);
			}
			catch (IOException e)
			{
				log.error("failed to make ssl connection to " + host + ":"
						+ port + " :" + e);

				return false;
			}
		}

		return false;
	}

	public boolean initiateConnection(int port, String host, boolean toLoadBalancer)
	{
		// make a connection to another server if remote hostname is supplied
		if (host != null)
		{
			try
			{
				outgoingConnection(new Socket(host, port));

				return true;
			}
			catch (UnknownHostException e)
			{
				log.info("Server establish ssl connection failed. Unknown Host: " + e.getMessage());

				System.exit(-1);
			}
			catch (IOException e)
			{
				log.error("Server failed to make plain connection to " + host + ":"
						+ port + " :" + e);

				return false;
			}
		}

		return false;
	}

	/*
	 * Processing incoming messages from the connection. Return true if the
	 * connection should close.
	 */
	public synchronized boolean process(Connection con, String msg)
	{
		return true;
	}

	/*
	 * The connection has been closed by the other party.
	 */
	public synchronized void connectionClosed(Connection con)
	{
		if (!term)
		{
			connections.remove(con);
		}
	}

	/*
	 * A new incoming connection has been established, and a reference is
	 * returned to it
	 */
	public synchronized Connection incomingConnection(Socket s) throws IOException
	{
		log.debug("incomming connection: " + Settings.socketAddress(s));
		
		Connection c = new Connection(s);
		connections.add(c);

		return c;
	}
	
	/*
	 * A new outgoing connection has been established, and a reference is
	 * returned to it
	 */
	public synchronized Connection outgoingConnection(Socket s) throws IOException
	{
		log.debug("outgoing connection: " + Settings.socketAddress(s));
		Connection c = new Connection(s);
		connections.add(c);
		
		return c;
	}
	
	@Override
	public void run()
	{
		log.info("using activity interval of " + Settings.getActivityInterval() + " milliseconds");

		while (!term)
		{
			// do something with 5 second intervals in between
			try
			{
				Thread.sleep(Settings.getActivityInterval());
			}
			catch (InterruptedException e)
			{
				log.info("received an interrupt, system is shutting down");
				
				break;
			}
			if (!term)
			{
				log.debug("doing activity");
				
				term = doActivity();
			}
		}

		log.info("closing " + connections.size() + " connections");

		// clean up
		for (Connection connection : connections)
		{
			connection.closeCon();
		}

		listener.setTerm(true);
	}

	public boolean doActivity()
	{
		return false;
	}

	public final void setTerm(boolean t)
	{
		term = t;
	}

	public final ArrayList<Connection> getConnections()
	{
		return connections;
	}
}
