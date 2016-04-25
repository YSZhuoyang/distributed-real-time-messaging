package Message;

public class LoginFailed extends JsonMessage
{
	private String info;

	public LoginFailed()
	{
		setCommand(JsonMessage.LOGIN_FAILED);
	}

	public void setInfo(String info)
	{
		this.info = info;
	}
}
