package activitystreamer.server;

import org.json.simple.JSONObject;

public class ServerLoad {
	private String id = null;
	private int load = 100000000;
	private Connection con;
	private int port = -1;
	private String hostname = null;
	
	ServerLoad(Connection con){
		setCon(con);
	}
	
	ServerLoad(Connection con, String hostname,int port){
		setCon(con);
		setHostname(hostname);
		setPort(port);
	}
	
	public void setId(String id) {
		this.id = id;
	}
	
	public void setLoad(int load) {
		this.load = load;
	}
	
	public void setPort(int port) {
		this.port = port;
	}
	
	public void setHostname(String hostname) {
		this.hostname = hostname;
	}
	
	private void setCon(Connection con) {
		this.con = con;
	}
	
	public String getId() {
		return id;
	}
	
	public int getLoad() {
		return load;
	}

	public int getPort() {
		return port;
	}
	
	public String getHostname() {
		return hostname;
	}
	
	public Connection getCon() {
		return con;
	}
}
