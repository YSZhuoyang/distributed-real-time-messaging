package Message;

public class RegisterFailedMsg extends JsonMessage
{
	private String info = "";

	public RegisterFailedMsg()
	{
		setCommand(JsonMessage.REGISTER_FAILED);
	}

	public void setInfo(String info)
	{
		this.info = info;
	}
}
