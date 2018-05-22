package activitystreamer.server;

import java.util.ArrayList;
import java.sql.Timestamp;

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
	
	public static boolean getHasBackup () {
		return hasBackup;
	}
	
	public static Connection getBackupCon () {
		return backupCon;
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
			System.out.println("found id: "+id);
			ServerList.update(id, obj, con);
			return false;
		}else if(!ServerList.isNewServer(con)) {
			System.out.println("found connection");
			ServerList.update(con, obj);
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
	public static void updateServerList(JSONObject recvOnj) {
		
	}
}

