package Message;


public class ActBroadMsg extends JsonMessage
{
	private Activity activity = new Activity();
	
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
		
		private void setActor(String a)
		{
			authenticated_user = a;
		}
	}
	
	public ActBroadMsg()
	{
		setCommand(JsonMessage.ACTIVITY_BROADCAST);
	}

	public void setActor(String a)
	{
		activity.setActor(a);
	}
	
	public void setObject(String obj)
	{
		activity.setObject(obj);
	}
}
