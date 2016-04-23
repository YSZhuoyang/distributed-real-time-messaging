package Message;

public class LoginSuccMsg extends JsonMessage
{
	private String info = "";

	public LoginSuccMsg()
	{
		command = "LOGIN_SUCCESS";
	}

	public void setInfo(String i)
	{
		info = i;
	}
}
