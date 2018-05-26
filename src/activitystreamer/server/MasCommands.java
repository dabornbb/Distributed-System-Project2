package activitystreamer.server;

import java.util.ArrayList;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.sql.Timestamp;

import org.json.simple.JSONObject;
import org.json.simple.JSONArray;

import activitystreamer.util.Commands;
import activitystreamer.util.Settings;

public class MasCommands {
	static ArrayList<User> registeredUsers = new ArrayList<User>();
	static BlockingQueue<RegMsg> registrationQueue = new LinkedBlockingQueue<RegMsg>();
	private static Timestamp time = null;
	
	// set master server connection for backup server 
	private static Connection masCon = null;
	private static Connection backupCon = null;
	private static boolean hasBackup = false;
	
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
	
	public static ArrayList getUserList () {
		return registeredUsers;
	}
	
	public static boolean Authenticate(Connection con, JSONObject obj) {
		if (ServerList.isNewServer(con)) {
			try {
				String serversecret = obj.get("secret").toString();
				if (serversecret.equals(Settings.getSecret())) {
					if (hasBackup) {
						String hostname=obj.get("hostname").toString();
						int port = Integer.parseInt(obj.get("portnum").toString());
						String id = obj.get("id").toString();
						ServerList.addServer(con,id,hostname,port);
						ServerList.sortServerList_byLoad_fromLowest_toHighest();
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
		}catch(Exception e) {
			e.printStackTrace();
		}
		
		return false;
	}
	
	public static boolean Login(Connection con, JSONObject obj) {
		try {
			String username=obj.get("username").toString();
			if (username.equals("anonymous")) {
				try {
					String id = obj.get("server_id").toString();
					ServerLoad server = ServerList.redirectTo(id);
					if (server!=null) {
						Commands.redirect(con, server.getHostname(), server.getPort(),obj);
					}else {
						time = new Timestamp(System.currentTimeMillis());
						Commands.loginSuccess(con, obj);
					}
				}catch(NullPointerException e) {
					e.printStackTrace();
				}
				return false;
			}
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
					String id = obj.get("server_id").toString();
					ServerLoad server = ServerList.redirectTo(id);
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
	
	public static boolean deliverList(Connection con,JSONObject sendObj) {
		if(ServerList.isNewServer(con)) {
			Commands.invalidMsg(con, "Server Not in Group");
			return true;
		}
		JSONArray list = new JSONArray();
		int listsize = ServerList.length();
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
	}
}

class RegMsg{
	Connection con;
	JSONObject obj;
	public RegMsg(Connection con,JSONObject obj) {
		this.con = con;
		this.obj = obj;
	}
	
	public Connection getCon() {
		return this.con;
	}
	public JSONObject getObj(){
		return this.obj;
	}
	
}
class RegProcessor extends Thread{
	BlockingQueue<RegMsg> queue;
	RegMsg msg;
	public RegProcessor(BlockingQueue<RegMsg> q) {
		this.queue = q;
	}
	
	@Override
	public void run() {

		try {
			while(true) {
				msg = queue.take();
				MasCommands.Register(msg.getCon(), msg.getObj());
			}
		}catch(Exception e) {
			e.printStackTrace();
		}
	}
}