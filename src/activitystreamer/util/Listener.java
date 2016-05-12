package activitystreamer.util;

import java.io.IOException;

import javax.net.ssl.SSLServerSocket;
import javax.net.ssl.SSLServerSocketFactory;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import activitystreamer.util.Settings;


public class Listener extends Thread
{
	protected static final Logger log = LogManager.getLogger();
	protected static Listener listener;
	
	protected SSLServerSocket sslServerSocket = null;
	protected boolean term = false;
	protected int portnum;

	protected Listener() throws IOException
	{
		portnum = Settings.getLocalPort();	// keep our own copy in case it
											// changes later
		
		// Initialize key store with certificate
		System.setProperty("javax.net.ssl.keyStore", "./Certificate/mykey");
		System.setProperty("javax.net.ssl.keyStorePassword", "keypass");
		System.setProperty("javax.net.debug", "all");
		
		// Generate ssl socket
		SSLServerSocketFactory factory = (SSLServerSocketFactory) SSLServerSocketFactory.getDefault();
		sslServerSocket = (SSLServerSocket) factory.createServerSocket(portnum);
		
		// Enable all security suites
		String[] suites = sslServerSocket.getSupportedCipherSuites();
		sslServerSocket.setEnabledCipherSuites(suites);
		
		start();
	}

	@Override
	public void run()
	{
		
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
