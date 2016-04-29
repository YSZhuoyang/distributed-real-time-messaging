package Message;

public class Activity
{
	private String type = "say";
	private String object = "";
	private String authenticated_user = "";
	
	
	public Activity()
	{
		setType("says");
	}

	public void setObject(String obj)
	{
		object = obj;
	}
	
	public void setActor(String a)
	{
		authenticated_user = a;
	}
	
	private void setType(String t)
	{
		type = t;
	}
}
