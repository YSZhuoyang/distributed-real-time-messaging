package activitystreamer.client;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JTextField;

import activitystreamer.util.Settings;


public class MainFrame implements ActionListener
{
	private JFrame frame;
	private JButton loginButton;
	private JButton registerButton;
	private JTextField userText;
	private JTextField hostnameText;
	private JTextField hostportText;
	private JPasswordField passwordText;
	private JButton anonymousButton;

	private ClientSolution clientThread;
	
	public MainFrame()
	{
		frame = new JFrame("User login");
		frame.setSize(450, 200);
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
		hostportText.setText("" + Settings.getRemotePort());
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

		loginButton = new JButton("Login");
		loginButton.setBounds(10, 130, 110, 25);
		panel.add(loginButton);
		loginButton.addActionListener(this);

		registerButton = new JButton("Register");
		registerButton.setBounds(140, 130, 110, 25);
		panel.add(registerButton);
		registerButton.addActionListener(this);

		anonymousButton = new JButton("Anonymous login");
		anonymousButton.setBounds(270, 130, 160, 25);
		panel.add(anonymousButton);
		anonymousButton.addActionListener(this);
		frame.setVisible(true);
		
		clientThread = ClientSolution.getInstance();
		clientThread.attachMainFrame(this);
	}
	
	public void showInfoBox(String error)
	{
        JOptionPane.showMessageDialog(null, error, "Info", JOptionPane.INFORMATION_MESSAGE);
	}
	
	public void hide()
	{
		frame.setVisible(false);
	}
	
	public void close()
	{
		frame.dispose();
	}
	
	public void actionPerformed(ActionEvent e)
	{
		if (e.getSource() == loginButton)
		{
			if (passwordText.getPassword().length == 0 ||
				userText.getText().isEmpty() ||
				hostportText.getText().isEmpty()||
				hostnameText.getText().isEmpty())
			{
				showInfoBox("All fields must not be empty");
				
				return;
			}
			
			Settings.setSecret(new String(passwordText.getPassword()));
			Settings.setUsername(userText.getText());
			Settings.setRemotePort(Integer.parseInt(hostportText.getText()));
			Settings.setRemoteHostname(hostnameText.getText());
			
			clientThread.establishConnection();
			clientThread.sendLoginMsg();
			clientThread.start();
		}
		else if (e.getSource() == registerButton)
		{
			if (passwordText.getPassword().length == 0 ||
				userText.getText().isEmpty() ||
				hostportText.getText().isEmpty()||
				hostnameText.getText().isEmpty())
			{
				showInfoBox("All fields must not be empty");
				
				return;
			}
			
			Settings.setSecret(new String(passwordText.getPassword()));
			Settings.setUsername(userText.getText());
			Settings.setRemotePort(Integer.parseInt(hostportText.getText()));
			Settings.setRemoteHostname(hostnameText.getText());
			
			clientThread.establishConnection();
			clientThread.sendRegisterMsg();
			clientThread.start();
		}
		else if (e.getSource() == anonymousButton)
		{
			clientThread.establishConnection();
			clientThread.sendAnonymusLoginMsg();
			clientThread.start();
		}
	}

	public void infoBox(String infoMessage, String titleBar)
	{
		JOptionPane.showMessageDialog(null, infoMessage, "InfoBox: " + titleBar, JOptionPane.INFORMATION_MESSAGE);
	}
}
