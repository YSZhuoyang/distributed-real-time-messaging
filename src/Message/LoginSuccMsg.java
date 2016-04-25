package Message;

public class LoginSuccMsg extends JsonMessage
{
	private String info = "";

	public LoginSuccMsg()
	{
		setCommand(JsonMessage.LOGIN_SUCCESS);
	}

	public void setInfo(String i)
	{
		info = i;
	}
}
