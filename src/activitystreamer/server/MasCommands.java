package activitystreamer.server;

import java.util.ArrayList;
import java.sql.Timestamp;
import java.net.Socket;
import java.io.IOException;

import org.json.simple.JSONObject;
import org.json.simple.JSONArray;

import activitystreamer.util.Commands;
import activitystreamer.util.Settings;

public class MasCommands {
	static ArrayList<User> registeredUsers = new ArrayList<User>();
	private static Timestamp time = null;
	
	// set master server connection for backup server 
	private static Connection masCon = null;
	private static Connection backupCon = null;
	private static boolean hasBackup = false;
	
	public static boolean isMaster(Connection con) {
		return masCon.equals(con);
	}
	public static boolean isBackup(Connection con) {
		return backupCon.equals(con);
	}
	public static boolean getHasBackup () {
		return hasBackup;
	}
	
	public static void setHasBackup (boolean b) {
		hasBackup = b;
	}
	
	public static void setBackup (Connection con) {
		backupCon = con;
	}
	
	public static Connection getBackupCon () {
		return backupCon;
	}
	
	public static Connection getMasterCon () {
		return masCon;
	}
	
	public static void setMasterCon (Connection con) {
		masCon = con;
	}
	
	public static ArrayList getUserList () {
		return registeredUsers;
	}
	
	public static boolean Authenticate(Connection con, JSONObject obj) {
		if (ServerList.isNewServer(con)) {
			try {
				String serversecret = obj.get("secret").toString();
				if (serversecret.equals(Settings.getSecret())) {
					if (hasBackup) {
						ServerList.addServer(con);
						time = new Timestamp(System.currentTimeMillis());
						System.out.println(time.getTime());
						Commands.AuthenSuccess(con, time);
						return false;
					} else {
						hasBackup = true;
						backupCon = con;
						time = new Timestamp(System.currentTimeMillis());
						System.out.println(time.getTime());
						Commands.AuthenSuccess(con, time);
						Commands.sendPromotion(con);
						ServerList.deleteServer(con);
						return false;
					}
				}else {
					Commands.authenFail(con, serversecret);
					return true;
				}
			}catch(NullPointerException e) {
				Commands.invalidMsg(con, "secret missing");
				return true;
			}
			
		}
		return false;
	
	}
	
	public static boolean Register(Connection con,JSONObject obj) {
		if(ServerList.isNewServer(con)) {
			Commands.invalidMsg(con, "Server Not in Group");
			return true;
		}
		try {
			String username=obj.get("username").toString();
			if (isRegistered(username)) {
				Commands.registerFail(con, obj);
			}else {
				String secret = obj.get("secret").toString();
				User user = new User(username, secret);
				registeredUsers.add(user);
				Commands.registerSuccess(con, obj);
			}
			
		}catch(NullPointerException e) {
			Commands.invalidMsg(con, e+": Incomplete Register Information");
		}
		
		return false;
	}
	
	public static boolean Login(Connection con, JSONObject obj) {
		if(ServerList.isNewServer(con)) {
			Commands.invalidMsg(con, "Server Not in Group");
			return true;
		}
		try {
			String username=obj.get("username").toString();
			if (!isRegistered(username)) {
				Commands.loginFail(con, "User not registered in the system",obj);
			}else {
				String secret;

				try {
					secret = obj.get("secret").toString();
				}catch(NullPointerException e) {
					return Commands.invalidMsg(con, "unknown disorder during signal transfer");
				}
				//------------start-----------------//

				//registered and correct secret
				if (secret.equals(getSecret(username))){
					// at successful login, run redirection
					ServerLoad server = ServerList.redirectTo(con);
					if (server!=null) {
						Commands.redirect(con, server.getHostname(), server.getPort(),obj);
					}else {
						time = new Timestamp(System.currentTimeMillis());
						Commands.loginSuccess(con, obj);
					}
				}else {
					Commands.loginFail(con, "Attempt to login with wrong secret", obj);
				}
			}
		}catch(NullPointerException e) {
			Commands.loginFail(con, e+": Incomplete Login Information",obj);
		}
		return false;
	}
	
	public static boolean deliverList(Connection con) {
		if(ServerList.isNewServer(con)) {
			Commands.invalidMsg(con, "Server Not in Group");
			return true;
		}
		JSONArray list = new JSONArray();
		int listsize = ServerList.length();
		JSONObject sendObj = new JSONObject();
		sendObj.put("command", "CONTACT_LIST");
		sendObj.put("size", listsize);
		if (listsize!=0) {
			for (ServerLoad sl:ServerList.serverList) {
				list.add(sl.toJSON());
			}
			sendObj.put("contact_list", list);
		}
		con.writeMsg(sendObj.toJSONString());
		return false;
	}
	
	public static boolean updateLoad(Connection con, JSONObject obj) {
		String id = obj.get("id").toString();
		if (!ServerList.isNewServer(id)) {
			ServerList.update(id, obj, con);
			ServerList.sortServerList_byLoad_fromLowest_toHighest();
			return false;
		}else if(!ServerList.isNewServer(con)) {
			ServerList.update(con, obj);
			ServerList.sortServerList_byLoad_fromLowest_toHighest();
			return false;
		}else if(con.equals(backupCon)) {
			//System.out.println("there are online users in backup that are not redirected yet");
			//System.out.println("backup: "+obj.toString());
			return false;
		}
		return !Commands.invalidMsg(con, "unauthenticated server");
	}
	
	private static boolean isRegistered(String username) {
		for (User user : registeredUsers) {
			if (username.equals(user.getUsername())) return true;
		}
		return false;
	}
	
	private static String getSecret(String username) {
		for (User user:registeredUsers) {
			if (user.getUsername().equals(username)){
				return user.getSecret();
			}
		}
		return null;
	}
	
	public static void deleteServer(Connection con) {
		ServerList.deleteServer(con);
	}
	
	public static int getNumOfChild() {
		return ServerList.length();
	}
	public static void updateServerList(JSONObject recvObj) {
		ArrayList <ServerLoad> svr = new ArrayList();
		JSONArray arr = (JSONArray) recvObj.get("server list");
		JSONObject obj;
		String id, h;
		int p, l;
		for (int i=0; i<arr.size(); i++) {
			obj = (JSONObject) arr.get(i);
			id = (String) obj.get("id");
			h = (String) obj.get("hostname");
			p = ((Long) obj.get("port")).intValue();
			l = ((Long) obj.get("load")).intValue();
			ServerLoad sl = new ServerLoad(null, id, h, p, l);
			svr.add(sl);
			System.out.println("added to server list: "+sl.objToString());
		}	
		ServerList.setServerList(svr);
	}
	public static void updateUserList(JSONObject recvObj) {
		ArrayList <User> usr = new ArrayList();
		JSONArray arr = (JSONArray) recvObj.get("user list");
		JSONObject obj;
		String un, sc;
		for (int i=0; i<arr.size(); i++) {
			obj = (JSONObject) arr.get(i);
			un = (String) obj.get("username");
			sc = (String) obj.get("secret");
			User u = new User(un, sc);
			usr.add(u);
			System.out.println("added to user list: "+u.objToString());
		}	
		registeredUsers = usr;
	}
	public static void contactChildServer(ServerLoad sl) {
		try {
			Connection con = Control.getInstance().outgoingConnection(new Socket(sl.getHostname(),sl.getPort()));
			Commands.sendMasterBroadcast(con);
			//Control.getInstance().removeMasterConnectionList(con);
		} catch (IOException e) {
			
		}
	}
	// this method is used by newly promoted to redirect all online users to other child servers
	public static void redirectAllOnlineUsers() {
		int indexC = 0, indexS = 0;
		int numOfClients = ChildCommands.onlineLength();
		int numOfServers = ServerList.getServerList().size();
		while (indexC<numOfClients && indexS<numOfServers) {
			ServerLoad sl = ServerList.getServerList().get(indexS);
			OnlineUser ou = ChildCommands.getOnlineUsers().get(indexC);
			JSONObject obj = new JSONObject();
			Commands.redirect(ou.getCon(), sl.getHostname(), sl.getPort(), obj);
			indexC++;
			indexS++;
			if (indexS>=numOfServers) 
				indexS = 0;
		}
 	}
}

