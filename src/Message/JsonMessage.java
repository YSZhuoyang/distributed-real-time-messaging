package Message;

import com.google.gson.Gson;


/*
 * Standardize format and content of Json message going between
 * different servers and clients, to minimize raising errors and
 * validations
 */
public class JsonMessage
{
	private String command = "";
	
	public final static String LOGIN = "LOGIN";
	public final static String LOGIN_SUCCESS = "LOGIN_SUCCESS";
	public final static String LOGIN_FAILED = "LOGIN_FAILED";
	public final static String LOGOUT = "LOGOUT";
	
	public final static String REGISTER = "REGISTER";
	public final static String REGISTER_SUCCESS = "REGISTER_SUCCESS";
	public final static String REGISTER_FAILED = "REGISTER_FAILED";
	
	public final static String ACTIVITY_BROADCAST = "ACTIVITY_BROADCAST";
	public final static String ACTIVITY_MESSAGE = "ACTIVITY_MESSAGE";
	
	public final static String AUTHENTICATION_FAIL = "AUTHTENTICATION_FAIL";
	public final static String AUTHENTICATE = "AUTHENTICATE";
	
	public final static String LOCK_ALLOWED = "LOCK_ALLOWED";
	public final static String LOCK_DENIED = "LOCK_DENIED";
	public final static String LOCK_REQUEST = "LOCK_REQUEST";

	public final static String REDIRECT = "REDIRECT";
	public final static String INVALID_MESSAGE = "INVALID_MESSAGE";
	public final static String SERVER_ANNOUNCE = "SERVER_ANNOUNCE";
	
	public final static String UNAUTHENTICATED_SERVER = "This is an unauthenticated server!!";
	
	
	public JsonMessage()
	{
		
	}
	
	protected void setCommand(String c)
	{
		command = c;
	}
	
	public String toJsonString()
	{
		return new Gson().toJson(this);
	}
}
