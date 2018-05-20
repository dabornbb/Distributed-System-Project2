package activitystreamer.server;

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

}
