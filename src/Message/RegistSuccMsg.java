package Message;

public class RegistSuccMsg extends JsonMessage
{
	private String info = "";

	public RegistSuccMsg()
	{
		setCommand(JsonMessage.REGISTER_SUCCESS);
	}

	public void setInfo(String info)
	{
		this.info = info;
	}
}
