package activitystreamer.server;

public class OnlineUser {
	private String username;
	private String socket;
	private Connection con;
	OnlineUser(String username,Connection con){
		this.username = username;
		socket = con.getSocket().toString();
		this.con = con;
	}
	
	public String getUsername() {
		return username;
	}
	
	public String getSocket() {
		return socket;
	}
	
	public Connection getCon() {
		return con;
	}

}
