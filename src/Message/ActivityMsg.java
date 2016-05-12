package Message;


public class ActivityMsg extends JsonMessage
{
	private Activity activity = new Activity();
	private String username = "";
	private String secret = "";
	private String id = "";

	class Activity
	{
		private String object = "";
		private String authenticated_user = "";
		
		
		public Activity()
		{
			
		}

		public void setObject(String obj)
		{
			object = obj;
		}
		
		public void setActor(String a)
		{
			authenticated_user = a;
		}
	}
	
	public ActivityMsg()
	{
		setCommand(JsonMessage.ACTIVITY_MESSAGE);
	}
	
	public void setObject(String obj)
	{
		activity.setObject(obj);
	}
	
	public void setId(String id)
	{
		this.id = id;
	}
	
	public void setUsername(String u)
	{
		username = u;
	}
	
	public void setSecret(String s)
	{
		secret = s;
	}
}
