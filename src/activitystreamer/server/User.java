package activitystreamer.server;

import org.json.simple.JSONObject;

public class User {
	private String username;
	private String secret;
	User(String username, String secret){
		this.username = username;
		this.secret = secret;
	}
	
	public String getUsername() {
		return username;
	}
	public String getSecret() {
		return secret;
	}
	
	public JSONObject objToString(){
		JSONObject obj = new JSONObject();
		obj.put("username", username);
		obj.put("secret", secret);
		return obj;
	}
}
