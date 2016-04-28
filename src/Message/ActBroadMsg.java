package Message;


public class ActBroadMsg extends JsonMessage
{
	private Activity activity = new Activity();
	
	class Activity
	{
		private String type = "say";
		private String object = "";
		private String actor = "";
		
		
		public Activity()
		{
			setType("say");
		}

		public void setObject(String obj)
		{
			object = obj;
		}
		
		public void setActor(String a)
		{
			actor = a;
		}
		
		private void setType(String t)
		{
			type = t;
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
