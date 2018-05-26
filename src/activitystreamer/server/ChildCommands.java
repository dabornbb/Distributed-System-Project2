package activitystreamer.server;

import org.json.simple.*;

import java.util.ArrayList;
import java.sql.Timestamp;
import java.net.Socket;
import java.io.IOException;

import activitystreamer.util.Commands;
import activitystreamer.util.Settings;


public class ChildCommands {
	
	private static Connection masCon = null;
	
	private static ArrayList<OnlineUser> onlineUsers = new ArrayList<OnlineUser>();
	public static ArrayList<Connection> connections = new ArrayList<Connection>();// store un-authenticated clients
	
	private static Timestamp time;
	private static long timeInt = 0;
	
	public static void setTimeInterval(JSONObject obj) {
		try {
			long mTime = Long.parseLong(obj.get("time").toString());
			time = new Timestamp(System.currentTimeMillis());
			timeInt = mTime - time.getTime();
		}catch(Exception e) {}
	}
	
	/* msg LOGIN received from -> client, msg sent to -> Client/ Mserver/ Mserver & Client, 
	 * function: pre-process anonymous, or direct to Mserver*/
	public static boolean Login(Connection con,JSONObject obj) {
		String username = obj.get("username").toString();

		obj.put("server_id", Settings.getServerId());
		client2MServer(con,obj);
		connections.add(con);
//		if (username.equals("anonymous")) {
//			time = new Timestamp(System.currentTimeMillis()+timeInt);
//			OnlineUser user = new OnlineUser(username,con,time);
//			onlineUsers.add(user);
//			Commands.updateLoad(masCon, Settings.getServerId(), onlineUsers.size());
//			time = new Timestamp(System.currentTimeMillis()+timeInt);
//			Commands.loginSuccess(con);
//		}else {
//		}
		return false;

	}
	
	/* msg LOGIN_SUCCESS received from -> Mserver, msg sent to -> Mserver & Client
	 * function: add the user into the onlineUser list*/
	public static void logUser(JSONObject obj) {
		String connection = obj.get("connection").toString();
		int connectionat = 0;
		for (Connection con:connections) {
			if (con.toString().equals(connection)) {
				String username = obj.get("username").toString();
				time = new Timestamp(System.currentTimeMillis()+timeInt);
				obj.remove("secret");
				obj.remove("server_id");
				obj.remove("connection");
				con.writeMsg(obj.toString());
				OnlineUser user = new OnlineUser(username,con,time);
				onlineUsers.add(user);
				Commands.updateLoad(masCon, Settings.getServerId(), onlineUsers.size());
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
		Commands.sendAuthenticate(con,Settings.getServerId(),Settings.getLocalHostname(),Settings.getLocalPort());
	}
	
	
	public static void getServerList(Connection con, JSONObject toServer) {
		for (OnlineUser user:onlineUsers) {
//		if (!user.getCon().equals(con))
		user.getCon().writeMsg(toServer.toString());
	}
		toServer.put("command", "BROADCAST_REQUEST");
		masCon.writeMsg(toServer.toJSONString());
		
	}
	public static void promoteToNewRank(JSONObject obj) {
		if (obj.get("newRank").equals("backup")){
			Settings.setServerType("b");
			System.out.println("I am now BACKUP");
		}
	}
	
	//broadcast message to all other servers
	public static void broadcastMsg(JSONObject obj) {
		
		// parse the json object to a list of serverload
		ArrayList<ServerLoad> serverList = new ArrayList();
		JSONArray contactlist = (JSONArray) obj.get("contact_list");
		JSONObject contact;
		String hostname;int portnum;
		for (int i=0;i< contactlist.size();i++) {
			contact = (JSONObject) contactlist.get(i);
			hostname=contact.get("hostname").toString();
			portnum = Integer.parseInt(contact.get("port").toString());
			ServerLoad sl = new ServerLoad(null,null,hostname,portnum);
			if (!(hostname.equals(Settings.getLocalHostname())&& portnum==Settings.getLocalPort()))
				serverList.add(sl);
			
		}
		
		// send message to all these servers
		try {
			obj.remove("serverList");
			obj.put("command", "ACTIVITY_BROADCAST");
			for (ServerLoad server:serverList) {
				Connection con = Control.getInstance().outgoingConnection(new Socket(server.getHostname(),server.getPort()));
				con.writeMsg(obj.toString());
			}
			
		}catch(IOException e) {
			e.printStackTrace();
		}

	}
	
	public static void broadcast2Clients(JSONObject obj) {
		obj.put("command", "ACTIVITY_MESSAGE");
		for (OnlineUser user:onlineUsers) {
			user.getCon().writeMsg(obj.toJSONString());
		}
	}

}
