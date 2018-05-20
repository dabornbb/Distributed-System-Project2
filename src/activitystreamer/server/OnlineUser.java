package activitystreamer.server;

import java.sql.Timestamp;
public class OnlineUser {
	private String username;
	private String socket;
	private Connection con;
	private Timestamp time;
	OnlineUser(String username,Connection con,Timestamp time){
		this.username = username;
		socket = con.getSocket().toString();
		this.con = con;
		this.time = time;
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
