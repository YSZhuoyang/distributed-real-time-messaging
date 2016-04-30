package activitystreamer.client;

import java.awt.event.ActionEvent;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JTextField;

import com.google.gson.Gson;
import com.google.gson.JsonObject;

import activitystreamer.util.Settings;

import java.awt.event.ActionListener;
public class loginFrame extends JFrame implements ActionListener{
	private JButton loginButton;
	private JButton registerButton;
	private JTextField userText;
	private JTextField hostnameText;
	private JTextField hostportText;
	private JFrame frame;
	private final static int DEFAULT_PSWD_CHARS = 10;
	private JTextField nameField = new JTextField(DEFAULT_PSWD_CHARS);
	final JPasswordField pswdField = new JPasswordField(DEFAULT_PSWD_CHARS);
	private JPasswordField passwordText;
	private JButton anonymousButton;
	public loginFrame(){
		frame = new JFrame("User login");
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
	public void closeFrame(){
		frame.setVisible(false);
	}
	public void actionPerformed(ActionEvent e)
	{
		if (e.getSource() == loginButton)
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
			ClientSolution.getInstance().establishConnection();
			ClientSolution.getInstance().sendRegisterMsg();
	
		}
		else if (e.getSource() == anonymousButton)
		{
			ClientSolution.getInstance().establishConnection();
			ClientSolution.getInstance().sendAnonymusLoginMsg();
		}
	}
	
	public void infoBox(String infoMessage, String titleBar)
    {
        JOptionPane.showMessageDialog(null, infoMessage, "InfoBox: " + titleBar, JOptionPane.INFORMATION_MESSAGE);
    }
}
