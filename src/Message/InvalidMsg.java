package Message;

public class InvalidMsg extends JsonMessage
{
	private String info = "";

	public InvalidMsg()
	{
		setCommand(JsonMessage.INVALID_MESSAGE);
	}

	public void setInfo(String info)
	{
		this.info = info;
	}
}
