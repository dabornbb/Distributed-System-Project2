package activitystreamer.server;

import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import activitystreamer.util.Commands;
import activitystreamer.util.Settings;


public class ServerCom {

	
	// receiving an authentication from a new server has no more than the connection
	public static boolean authenticate(Connection con,JSONObject obj) {
		if (!ServerList.isNewServer(con)) {
			Commands.invalidMsg(con, "Server already authenticated");
			
			return false;
		}
		try {
			String serversecret = obj.get("secret").toString();
			if (serversecret.equals(Settings.getSecret())) {
				ServerList.addServer(con);
				System.out.println("[CONTACT LIST] "+ServerList.serverList.size());
				return true;
			}else {
				return Commands.authenFail(con, serversecret);
			}
		}catch(NullPointerException e) {
			return Commands.invalidMsg(con, "secret missing");
		}
	}

	public static void sendAnnounce() {
		int load = Login.connections.size();
		String serverId = Settings.getServerId();
		String lh = Settings.getLocalHostname();
		int port = Settings.getLocalPort();
		JSONObject obj = new JSONObject();
		obj.put("command", "SERVER_ANNOUNCE");
		obj.put("id", Settings.getServerId());
		obj.put("load", load);
		obj.put("hostname", lh);
		obj.put("port", port);
		System.out.println("[ANNOUNCE] "+obj.toJSONString());
		ServerCom.broadcastAll(obj);
	}
	
	public static void sendAuthenticate(Connection con) {
		Commands.sendAuthenticate(con);
		String hostname = Settings.getRemoteHostname();
		int port = Settings.getRemotePort();
		ServerList.addServer(con,hostname,port);
	}
	
	public static void authenFail(Connection con) {
		ServerList.deleteServer(con);
	}
	
	public static boolean updateAnnouce(Connection con,JSONObject obj) {
		String id = obj.get("id").toString();
		if (!ServerList.isNewServer(id)) {
			ServerList.update(id, obj,con);
			return true;
		}else if(!ServerList.isNewServer(con)) {
			ServerList.update(con, obj);
			return true;
		}
		return Commands.invalidMsg(con, "unauthenticated server");
		
	}
	
	public static boolean activityBroadcast(Connection con, JSONObject obj) {
		System.out.println("accessed serverCom.acBroadcast");
		if (ServerList.isNewServer(con) && !Login.isLogin(con)) 
			return Commands.invalidMsg(con, "Unauthenticated server.");
		broadcastExcept(con,obj);
		obj.put("command", "ACTIVITY_MESSAGE");
		broadcastExceptClient(con,obj.toJSONString());
		
		return true;
	}
	
	public static void broadcastExcept(Connection con,JSONObject obj) {
		System.out.println("accessed Servercom.bcExcept");
		System.out.println("size of serverlist "+ServerList.serverList.size());
		for (ServerLoad server: ServerList.serverList) {
			if (!server.getCon().getSocket().toString().equals(con.getSocket().toString())) {
				System.out.println("sending msg to "+server.getPort()+":"+obj.toJSONString());
				server.getCon().writeMsg(obj.toString());
			}
		}
	}
	
	public static void broadcastExceptClient(Connection con, String msg) {
		for (OnlineUser user:Login.onlineUsers) {
			if(!user.getSocket().toString().equals(con.getSocket().toString())) {
				System.out.println("user socket: "+user.getSocket().toString());
				System.out.println("incoming socket: "+con.getSocket().toString());
				
				user.getCon().writeMsg(msg);
			}
		}
	}
	//send lock command to other servers, replace the lock with a command
	//used by request (con,obj,"LOCK_REQUEST"), broadcast
	//reaction of LOCK_REQUEST,LOCK_ALLOWED,LOCK_DENIED
	//server -> server communication
	public static boolean broadcastLock(Connection con, JSONObject obj) {
		System.out.println("[LOCK] accessed ServerCom.bcLock");
		if (ServerList.isNewServer(con)) return Commands.invalidMsg(con, "Unauthenticated server");
		String objCmd = obj.get("command").toString();
		String username,secret;
		String reactCmd = objCmd;
		try {
			username = obj.get("username").toString();
			secret = obj.get("secret").toString();
			
			//if this is a "LOCK DENIED" object, delete user or broadcast it
			 if (objCmd.equals("LOCK_DENIED")) {
				 System.out.println("[LOCK] accessed ServerCom.bcLock.LockDenied");
				 if (Login.isLogin(username)) {
					 System.out.println("[LOCK] LOCK_DENIED. Sending...");
					 Commands.registerFail(Login.getCon(username), username);
					 Login.deleteUser(username, secret);
					 Login.logoutUser(username);
					 return true;
				 }else {
					 broadcastExcept(con,obj);
				 }
			 }
			 
			 //if this is a "LOCK_REQUEST" object, react with denied or allowed 
			 else if (objCmd.equals("LOCK_REQUEST")) {
				System.out.println("[LOCK] accessed ServerCom.bcLock.LockRequest");
				if (Login.isRegistered(username)) {
					obj.put("command","LOCK_DENIED");
					broadcastAll(obj);
				}else {
					User newUser = new User(username,secret);
					Login.registeredUsers.add(newUser);
					obj.put("command", "LOCK_ALLOWED");
					broadcastAll(obj);
				}
				
			// if is a "LOCK_ALLOWED" object, broadcast to 
			}else if (objCmd.equals("LOCK_ALLOWED")) {
				broadcastExcept(con,obj);
			}
			return true;
		}catch (NullPointerException e) {
			return Commands.invalidMsg(con, "incorrect message at lock broadcast");
		}
	}

	
	public static void broadcastAll(JSONObject obj) {
		for (ServerLoad server: ServerList.serverList) {
			server.getCon().writeMsg(obj.toJSONString());
		}
	}
	
	
}
