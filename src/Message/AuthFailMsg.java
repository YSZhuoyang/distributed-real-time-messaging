package Message;

public class AuthFailMsg extends JsonMessage
{
	private String info = "";

	public AuthFailMsg()
	{
		setCommand(JsonMessage.AUTHENTICATION_FAIL);
	}
	
	public void setInfo(String i)
	{
		info = i;
	}
}
