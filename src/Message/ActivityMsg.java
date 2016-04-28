package Message;

//import com.ibm.common.activitystreams.Activity;

public class ActivityMsg extends JsonMessage
{
	private Activity activity = new Activity();
	private String username = "";
	private String secret = "";

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
	
	public void setActor(String a)
	{
		activity.setActor(a);
	}
	
	public void setObject(String obj)
	{
		activity.setObject(obj);
	}
	
	public void setUserActivity(Activity a)
	{
		activity = a;
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
