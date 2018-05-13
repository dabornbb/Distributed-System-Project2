package activitystreamer.server;

import org.json.simple.JSONObject;

import activitystreamer.util.Commands;
import activitystreamer.util.Settings;

import java.util.ArrayList;
public class Login {
	static ArrayList<User> registeredUsers = new ArrayList<User>();
	static ArrayList<OnlineUser> onlineUsers = new ArrayList<OnlineUser>();
	static ArrayList<Connection> connections = new ArrayList<Connection>();
/*	public static boolean loginUser(Connection con, JSONObject obj) {
		//not checking if the user is login with another client, uses connections as reference
		if (isLogin(con)) {
			return Commands.invalidMsg(con,"User already login, logging out...");
		}
		
		String username,secret;

		
		username = obj.get("username").toString();
		if (username.equals("anonymous")) {
			logAnonymous(con);
			ServerLoad server = ServerList.redirectTo();
			if ((server = ServerList.redirectTo())!=null) {
				JSONObject sendRedirect = new JSONObject();
				sendRedirect.put("command", "REDIRECT");
				sendRedirect.put("hostname", server.getHostname());
				sendRedirect.put("port", server.getPort());
				con.writeMsg(sendRedirect.toJSONString());
				con.closeCon();
				logoutUser(con);
				return false;
			}
			return true;
		}
			
		//not asking for redirection

		//-------Obj has username-------//
		// if Obj has secret, set it
		// else generate one and set it, then register
		try {
			secret = obj.get("secret").toString();
		}catch(NullPointerException e) {
			return Commands.invalidMsg(con, "unknown disorder during signal transfer");
		}
		
		
		//------------start-----------------//

		// if is a registered user
		if(isRegistered(username)) {
			//registered and correct secret
			if (secret.equals(getSecret(username))){

				connections.add(con);
				onlineUsers.add(new OnlineUser(username,con));
				// at successful login, run redirection
				Commands.loginSuccess(con, username);
				ServerLoad server = ServerList.redirectTo();
				if ((server = ServerList.redirectTo())!=null) {
					JSONObject sendRedirect = new JSONObject();
					sendRedirect.put("command", "REDIRECT");
					sendRedirect.put("hostname", server.getHostname());
					sendRedirect.put("port", server.getPort());
					con.writeMsg(sendRedirect.toJSONString());
					con.closeCon();
					logoutUser(con);
					return false;
				}
				
				return true;
			}else {
				Commands.loginFail(con, "Attempt to login with wrong secret");
				return false;
			}
		}
		Commands.loginFail(con, "User is not registered in the system");
		return false;
		
	}
*/
	private static void logAnonymous(Connection con) {
		System.out.println("Log in anonymous.");
		Commands.loginSuccess(con);
		if (!connections.contains(con)) {
			connections.add(con);
			onlineUsers.add(new OnlineUser("anonymous",con));
		}
	}

	
	public static boolean isLogin(Connection con) {
		for (OnlineUser user : onlineUsers) {
			if (user.getSocket().equals(con.getSocket().toString())) return true;
		}
		return false;
	}
	

	public static boolean isLogin(String username) {
		for (OnlineUser user:onlineUsers) {
			if (user.getUsername().equals(username))
				return true;
		}
		return false;
	}
	
	// used by client to server thus close with connection
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
			System.out.println("Client logged out.");
		}
		
	}
	
	// get username by connection
	public static String getUsername(Connection con) {
		for (OnlineUser user:onlineUsers) {
			if (user.getCon().equals(con))
				return user.getUsername();
		}
		return null;
	}
	
	//get connection by name
	public static Connection getCon(String username) {
		for (OnlineUser user:onlineUsers) {
			if (user.getUsername().equals(username)) {
				return user.getCon();
			}
		}
		return null;
	}
	
	// used by server closing actively thus close with username
	public static void logoutUser(String username) {
		int userat = 0;
		for (OnlineUser user:onlineUsers) {
			if (user.getUsername().equals(username))
				break;
			userat++;
		}
		if (userat<onlineUsers.size()) {
			Commands.logoutSuccess(onlineUsers.get(userat).getCon());
			onlineUsers.remove(userat);
			System.out.println("Client logged out.");
		}
	}
	
	public static boolean registerUser(Connection con, JSONObject obj) {
		String username = obj.get("username").toString();
		String secret = obj.get("secret").toString();
		// if user is registered to the server
		if (isRegistered(username)) {
			Commands.registerFail(con, username);
			con.closeCon();
			return false;
		}
		
		// if user is not registered, add to list and send lock request
		User user = new User(username,secret);
		registeredUsers.add(user);
		obj.put("command", "LOCK_REQUEST");
		ServerCom.broadcastAll(obj);
		Commands.registerSuccess(con, username, secret);
		obj.put("command","LOGIN");
		return false;
	}
	
	public static boolean isRegistered(String username) {
		for (User user : registeredUsers) {
			if (username.equals(user.getUsername())) return true;
		}
		return false;
	}

	
	//get secret by name
	public static String getSecret(String username) {
		for (User user:registeredUsers) {
			if (user.getUsername().equals(username)){
				return user.getSecret();
			}
		}
		return null;
	}

	public static void deleteUser(String username, String secret) {
		int userat = 0;
		for (User user:registeredUsers) {
			if (!user.getUsername().equals(username)&&user.getSecret().equals(secret))
				break;
			userat++;
		}
		registeredUsers.remove(userat);
	}

}
