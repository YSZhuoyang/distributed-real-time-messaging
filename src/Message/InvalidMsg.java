package Message;

public class InvalidMsg extends JsonMessage
{
	private String info = "";

	public InvalidMsg()
	{
		command = "INVALID_MESSAGE";
	}

	public void setInfo(String info)
	{
		this.info = info;
	}
}
