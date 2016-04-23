package Message;

public class RegistFaildMsh extends JsonMessage
{
	private String info = "";

	public RegistFaildMsh()
	{
		setCommand("REGISTER_FAILED");
		// TODO Auto-generated constructor stub
	}

	public void setInfo(String info)
	{
		this.info = info;
	}
}
