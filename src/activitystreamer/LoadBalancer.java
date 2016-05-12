package activitystreamer;

import java.net.InetAddress;
import java.net.UnknownHostException;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import activitystreamer.server.LoadBalancer.LoadBalancerSolution;
import activitystreamer.util.Settings;


public class LoadBalancer
{
	private static final Logger log = LogManager.getLogger();

	private static void help(Options options)
	{
		String header = "An ActivityStream Server for Unimelb COMP90015\n\n";
		String footer = "\ncontact aharwood@unimelb.edu.au for issues.";
		HelpFormatter formatter = new HelpFormatter();
		formatter.printHelp("ActivityStreamer.Server", header, options, footer, true);
		System.exit(-1);
	}

	public static void main(String[] args)
	{
		log.info("reading command line options");

		Options options = new Options();
		options.addOption("lp", true, "local port number");
		options.addOption("lh", true, "local hostname");
		options.addOption("a", true, "activity interval in milliseconds");

		// build the parser
		CommandLineParser parser = new DefaultParser();
		CommandLine cmd = null;
		
		try
		{
			cmd = parser.parse(options, args);
		}
		catch (ParseException e1)
		{
			help(options);
		}

		if (cmd.hasOption("lp"))
		{
			try
			{
				int port = Integer.parseInt(cmd.getOptionValue("lp"));
				Settings.setLocalPort(port);
			}
			catch (NumberFormatException e)
			{
				log.info("-lp requires a port number, parsed: " + cmd.getOptionValue("lp"));
				help(options);
			}
		}

		if (cmd.hasOption("a"))
		{
			try
			{
				int a = Integer.parseInt(cmd.getOptionValue("a"));
				Settings.setActivityInterval(a);
			}
			catch (NumberFormatException e)
			{
				log.error("-a requires a number in milliseconds, parsed: " + cmd.getOptionValue("a"));
				help(options);
			}
		}

		try
		{
			Settings.setLocalHostname(InetAddress.getLocalHost().getHostAddress());
		}
		catch (UnknownHostException e)
		{
			log.warn("failed to get localhost IP address");
		}

		if (cmd.hasOption("lh"))
		{
			Settings.setLocalHostname(cmd.getOptionValue("lh"));
		}

		log.info("starting server");
		
		final LoadBalancerSolution lbs = LoadBalancerSolution.getInstance();
		
		// the following shutdown hook doesn't really work, it doesn't give us
		// enough time to
		// cleanup all of our connections before the jvm is terminated.
		Runtime.getRuntime().addShutdownHook(new Thread()
		{
			public void run()
			{
				lbs.setTerm(true);
				lbs.interrupt();
			}
		});
	}

}
