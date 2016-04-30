package activitystreamer.client;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.border.Border;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

import activitystreamer.Client;
import activitystreamer.util.Settings;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

@SuppressWarnings("serial")
public class TextFrame extends JFrame implements ActionListener
{
	private static final Logger log = LogManager.getLogger();
	
	private JTextArea inputText;
	private JTextArea outputText;
	private JButton sendButton;
	private JButton loginButton;
	private JButton registerButton;
	private JButton disconnectButton;
	private JButton anonymousButton;
	private JPasswordField passwordText;
	private JTextField userText;
	private JTextField hostnameText;
	private JTextField hostportText;
	private JSONParser parser = new JSONParser();
	private final static int DEFAULT_PSWD_CHARS = 10;
	private JTextField nameField = new JTextField(DEFAULT_PSWD_CHARS);
	final JPasswordField pswdField = new JPasswordField(DEFAULT_PSWD_CHARS);

	public TextFrame(){
		JFrame frame = new JFrame("User login");
		frame.setSize(300,200);
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		frame.setLocationRelativeTo(null);
		JPanel panel = new JPanel();
		frame.add(panel);
		panel.setLayout(null);
		
		JLabel remoteHostLabel = new JLabel("host");
		remoteHostLabel.setBounds(10, 10, 80, 25);
		panel.add(remoteHostLabel);

		hostnameText = new JTextField(20);
		hostnameText.setText(Settings.getRemoteHostname());
		hostnameText.setBounds(100, 10, 160, 25);
		panel.add(hostnameText);

		JLabel portLabel = new JLabel("port");
		portLabel.setBounds(10, 40, 80, 25);
		panel.add(portLabel);

		hostportText = new JTextField(20);
		hostportText.setText(""+Settings.getRemotePort());
		hostportText.setBounds(100, 40, 160, 25);
		panel.add(hostportText);
		
		JLabel userLabel = new JLabel("User");
		userLabel.setBounds(10, 70, 80, 25);
		panel.add(userLabel);

		userText = new JTextField(20);
		userText.setText(Settings.getUsername());
		userText.setBounds(100, 70, 160, 25);
		panel.add(userText);

		JLabel passwordLabel = new JLabel("Secret");
		passwordLabel.setBounds(10, 100, 80, 25);
		panel.add(passwordLabel);

		passwordText = new JPasswordField(20);
		passwordText.setText(Settings.getSecret());
		passwordText.setBounds(100, 100, 160, 25);
		panel.add(passwordText);

		loginButton = new JButton("login");
		loginButton.setBounds(10, 130, 80, 25);
		panel.add(loginButton);
		loginButton.addActionListener(this);
		
		registerButton = new JButton("register");
		registerButton.setBounds(110, 130, 80, 25);
		panel.add(registerButton);
		registerButton.addActionListener(this);
		
		anonymousButton = new JButton("anonymous");
		anonymousButton.setBounds(210, 130, 80, 25);
		panel.add(anonymousButton);
		anonymousButton.addActionListener(this);
		frame.setVisible(true);
	}
	
	public void TextFrame()
	{
		setTitle("ActivityStreamer Text I/O");
		JPanel mainPanel = new JPanel();
		mainPanel.setLayout(new GridLayout(1, 2));
		JPanel inputPanel = new JPanel();
		JPanel outputPanel = new JPanel();
		inputPanel.setLayout(new BorderLayout());
		outputPanel.setLayout(new BorderLayout());
		Border lineBorder = BorderFactory.createTitledBorder(BorderFactory.createLineBorder(Color.lightGray),
				"JSON input, to send to server");
		inputPanel.setBorder(lineBorder);
		lineBorder = BorderFactory.createTitledBorder(BorderFactory.createLineBorder(Color.lightGray),
				"JSON output, received from server");
		outputPanel.setBorder(lineBorder);
		outputPanel.setName("Text output");

		inputText = new JTextArea();
		JScrollPane scrollPane = new JScrollPane(inputText);
		inputPanel.add(scrollPane, BorderLayout.CENTER);

		JPanel buttonGroup = new JPanel();
		sendButton = new JButton("Send");
		disconnectButton = new JButton("Disconnect");
		//loginButton = new JButton("Login");
		buttonGroup.add(sendButton);
		buttonGroup.add(disconnectButton);
		//buttonGroup.add(loginButton);
		inputPanel.add(buttonGroup, BorderLayout.SOUTH);
		sendButton.addActionListener(this);
		disconnectButton.addActionListener(this);
		//loginButton.addActionListener(this);

		outputText = new JTextArea();
		scrollPane = new JScrollPane(outputText);
		outputPanel.add(scrollPane, BorderLayout.CENTER);

		mainPanel.add(inputPanel);
		mainPanel.add(outputPanel);
		add(mainPanel);

		setLocationRelativeTo(null);
		setSize(1280, 768);
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		setVisible(true);
	}
	
	public void displayActivityMessageText(final JsonObject obj)
	{
		String newText = new Gson().toJson(obj);
		String oldText = outputText.getText();
		
		outputText.setText(oldText + "\n\n" + newText);
		outputText.revalidate();
		outputText.repaint();
	}
	
	public void showErrorMsg(String error)
	{
        JOptionPane.showMessageDialog(null, error, "Error", JOptionPane.INFORMATION_MESSAGE);
	}
	
	public void infoBox(String infoMessage, String titleBar)
    {
        JOptionPane.showMessageDialog(null, infoMessage, "InfoBox: " + titleBar, JOptionPane.INFORMATION_MESSAGE);
    }
	
	public void setOutputText(final JSONObject obj)
	{
		Gson gson = new GsonBuilder().setPrettyPrinting().create();
		JsonParser jp = new JsonParser();
		JsonElement je = jp.parse(obj.toJSONString());
		String prettyJsonString = gson.toJson(je);
		
		outputText.setText(prettyJsonString);
		outputText.revalidate();
		outputText.repaint();
	}

	@Override
	public void actionPerformed(ActionEvent e)
	{
		if (e.getSource() == sendButton)
		{
			String msg = inputText.getText().trim().replaceAll("\r", "").replaceAll("\n", "").replaceAll("\t", "");
			ClientSolution.getInstance().sendActivityObject(msg);
		}
		else if (e.getSource() == disconnectButton)
		{
			ClientSolution.getInstance().sendLogoutMsg();
			ClientSolution.getInstance().disconnect();
		}
		else if (e.getSource() == loginButton)
		{
			Settings.setSecret(passwordText.getText());
			Settings.setUsername(userText.getText());
			Settings.setRemotePort(Integer.parseInt(hostportText.getText()));
			Settings.setRemoteHostname(hostnameText.getText());
			ClientSolution.getInstance().establishConnection();
			ClientSolution.getInstance().sendLoginMsg();
		}
		else if (e.getSource() == registerButton)
		{
			Settings.setSecret(passwordText.getText());
			Settings.setUsername(userText.getText());
			Settings.setRemotePort(Integer.parseInt(hostportText.getText()));
			Settings.setRemoteHostname(hostnameText.getText());
			log.info(Integer.parseInt(hostportText.getText()));
			ClientSolution.getInstance().establishConnection();
			ClientSolution.getInstance().sendRegisterMsg();
		}
		else if (e.getSource() == anonymousButton)
		{
			ClientSolution.getInstance().establishConnection();
			ClientSolution.getInstance().sendAnonymusLoginMsg();
		}
	}
}
