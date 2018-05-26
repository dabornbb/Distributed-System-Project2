package activitystreamer.server;

import org.json.simple.*;

import java.util.ArrayList;
import java.sql.Timestamp;

import activitystreamer.util.Commands;
import activitystreamer.util.Settings;


public class ChildCommands {
	
	private static Connection masCon = null;
	
	private static ArrayList<OnlineUser> onlineUsers = new ArrayList<OnlineUser>();
	public static ArrayList<Connection> connections = new ArrayList<Connection>();// store un-authenticated clients
	
	private static Timestamp time;
	private static long timeInt = 0;
	
	public static ArrayList<OnlineUser> getOnlineUsers() {
		return onlineUsers;
	}
	
	public static void setTimeInterval(JSONObject obj) {
		try {
			long mTime = Long.parseLong(obj.get("time").toString());
			time = new Timestamp(System.currentTimeMillis());
			timeInt = mTime - time.getTime();
		}catch(Exception e) {}
	}
	
	/* msg received from -> client, msg sent to -> Client/ Mserver/ Mserver & Client, 
	 * function: pre-process anonymous, or direct to Mserver*/
	public static boolean Login(Connection con,JSONObject obj) {
		String username = obj.get("username").toString();

		if (username.equals("anonymous")) {
			time = new Timestamp(System.currentTimeMillis()+timeInt);
			OnlineUser user = new OnlineUser(username,con,time);
			onlineUsers.add(user);
			Commands.updateLoad(masCon, Settings.getServerId(), onlineUsers.size());
			time = new Timestamp(System.currentTimeMillis()+timeInt);
			Commands.loginSuccess(con);
		}else {
			client2MServer(con,obj);
			connections.add(con);
		}
		return false;

	}
	
	/* msg received from -> Mserver, msg sent to -> Mserver & Client
	 * function: add the user into the onlineUser list*/
	public static void logUser(JSONObject obj) {
		String connection = obj.get("connection").toString();
		int connectionat = 0;
		for (Connection con:connections) {
			if (con.toString().equals(connection)) {
				String username = obj.get("username").toString();
				time = new Timestamp(System.currentTimeMillis()+timeInt);
				con.writeMsg(obj.toString());
				OnlineUser user = new OnlineUser(username,con,time);
				onlineUsers.add(user);
				break;
			}
			connectionat ++;
		}
		connections.remove(connectionat);
	}
	
	/* msg received from -> client, msg sent to -> Mserver & Client*/
	public static void logoutUser(Connection con) {
		int userat = 0;
		for (OnlineUser user:onlineUsers) {
			if (user.getCon().equals(con))
				break;
			userat++;
		}
		if (userat<onlineUsers.size()) {
			onlineUsers.remove(userat);
			Commands.logoutSuccess(con);
			con.closeCon();
			Commands.updateLoad(masCon, Settings.getServerId(), ServerList.length());
			System.out.println("Client logged out.");
		}
	}
	
	/* msg received from -> client, msg sent to -> Mserver*/
	public static void client2MServer(Connection con, JSONObject obj) {
		obj.put("connection", con.toString());
		connections.add(con);
		masCon.writeMsg(obj.toJSONString());
	}
	
	/* msg received from -> MServer -> no change -> single client -> close connection*/
	public static void mServer2Client(JSONObject obj) {
		String connection = obj.get("connection").toString();
		int connectionat = 0;
		for (Connection con:connections) {
			if (con.toString().equals(connection)) {
				break;
			}
			connectionat++;
		}
		connections.get(connectionat).writeMsg(obj.toString());
		connections.get(connectionat).closeCon();
		connections.remove(connectionat);

	}
	
	public static void setMasterConnection(Connection con) {
		System.out.println("set mas con");
		masCon = con;
	}
	
	public static Connection getMasterConnection() {
		return masCon;
	}
	
	private static boolean isOnline(Connection con) {
		for (OnlineUser user:onlineUsers) {
			if (user.getCon().equals(con)) return true;
		}
		return false;
	}
	
	public static int onlineLength() {
		return onlineUsers.size();
	}
	
	public static void sendAuthenticate(Connection con) {
		Commands.sendAuthenticate(con);
	}
	
	public static void promoteToNewRank(Connection con, JSONObject obj) {
		if (obj.get("secret").equals(Settings.getSecret())) {
			if (obj.get("newRank").equals("backup")){
				Settings.setServerType("b");
				if (Settings.getServerType().equals("b"))
					System.out.println("I am now BACKUP");
				else 
					System.out.println("Current server type: " + Settings.getServerType());
			}
		} else {
			System.out.print("promotion failed due to WRONG secret");
			Commands.authenFail(con, (String) obj.get("secret"));
		}
	}
	// send authentication message to Master Server on startup
	public static void masAuthenticate() {
		// where is masCon created?
		Commands.sendAuthenticate(masCon);
	}
	
	public static Connection getMasCon (){
		return masCon;
	}
	
	public static void contactNewMaster(Connection con, JSONObject obj) {
		if (obj.get("secret").equals(Settings.getSecret())) {
			String hostname = (String) obj.get("host name");
			int port = ((Long) obj.get("port")).intValue();
			System.out.println("New master is at "+hostname+" : "+port);
			setMasterConnection(Control.getInstance().initiateConnectionToNewMaster(hostname, port));
		} else {
			System.out.print("promotion failed due to WRONG secret");
			Commands.authenFail(con, (String) obj.get("secret"));
		}
	}
}
