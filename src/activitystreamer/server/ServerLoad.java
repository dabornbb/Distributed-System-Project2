package activitystreamer.server;

import org.json.simple.JSONObject;

public class ServerLoad implements Comparable<ServerLoad>{
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
	
	ServerLoad(Connection con, String i, String h,int p, int l){
		setCon(con);
		setId(i);
		setHostname(h);
		setPort(p);
		setLoad(l);
	}
	public JSONObject objToString(){
		JSONObject obj = new JSONObject();
		obj.put("id", id);
		obj.put("load", load);
		obj.put("hostname", hostname);
		obj.put("port", port);
		return obj;
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
	
	public void setCon(Connection con) {
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
	
	public JSONObject toJSON() {
		JSONObject obj = new JSONObject();
		obj.put("ID", this.id);
		obj.put("hostname", this.hostname);
		obj.put("port", this.port);
		return obj;
	}
	
	// compareTo method to sort ServerLoad objects in ascending order by lowest load to highest load
	public int compareTo(ServerLoad compareLoad) {
	
		int comLoad = ((ServerLoad) compareLoad).getLoad(); 
		
		//ascending order
		return this.load - comLoad;
		
		//descending order
		//return compareQuantity - this.quantity;
		
	}
}
