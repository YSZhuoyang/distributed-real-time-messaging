package Message;

public class LogoutMsg extends RegisterMsg
{

	public LogoutMsg()
	{
		setCommand(JsonMessage.LOGOUT);
	}
}
