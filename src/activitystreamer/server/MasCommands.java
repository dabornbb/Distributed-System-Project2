package activitystreamer.server;

import java.util.ArrayList;

import org.json.simple.JSONObject;
import org.json.simple.JSONArray;

import activitystreamer.util.Commands;
import activitystreamer.util.Settings;
class MasCommands {
	static ArrayList<User> registeredUsers = new ArrayList<User>();
//	static ArrayList<ServerLoad> serverLoads = new ArrayList<ServerLoad>();
	
	public static boolean Authenticate(Connection con, JSONObject obj) {
		if (ServerList.isNewServer(con)) {
			try {
				String serversecret = obj.get("secret").toString();
				if (serversecret.equals(Settings.getSecret())) {
					ServerList.addServer(con);
					System.out.println("[CONTACT LIST] "+ServerList.serverList.size());
					Commands.AuthenSuccess(con);
					return false;
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
				Commands.registerFail(con, username);
			}else {
				String secret = obj.get("secret").toString();
				User user = new User(username, secret);
				registeredUsers.add(user);
				Commands.registerSuccess(con, username, secret);
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
				Commands.loginFail(con, "User not registered in the system");
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
						Commands.redirect(con, server.getHostname(), server.getPort());
					}else {
						Commands.loginSuccess(con, username);
					}
				}else {
					Commands.loginFail(con, "Attempt to login with wrong secret");
				}
			}
		}catch(NullPointerException e) {
			Commands.invalidMsg(con, e+": Incomplete Login Information");
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
			return false;
		}else if(!ServerList.isNewServer(con)) {
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
}

