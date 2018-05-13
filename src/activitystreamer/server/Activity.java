package activitystreamer.server;

import org.json.simple.JSONObject;

import activitystreamer.util.Commands;

public class Activity {
	//returns true if processed smoothly, returns false if hits an error
	//client -> server communication
	public static boolean actMsg(Connection con,JSONObject obj) {
		JSONObject failObj = new JSONObject();
		JSONObject clientObj = new JSONObject();
		failObj.put("command","AUTHENTICATION_FAIL");
		if (!Login.isLogin(con)) 
		{
			failObj.put("info", "User is not login");
			con.writeMsg(failObj.toString());
			con.closeCon();
			return false;
		}
		String username=null,secret= null,message=null;
		try {
			username=obj.get("username").toString();
		}catch(NullPointerException e) {
			return Commands.invalidMsg(con,"incomplete message");
		}

		
		// if is anonymous client, directly broadcast
		if (username.equals("anonymous")){
			ServerCom.broadcastExceptClient(con, obj.toJSONString());
			obj.put("command", "ACTIVITY_BROADCAST");
			ServerCom.broadcastAll(obj);
			return true;
		}
		
		// if not anonymous, 
		secret = obj.get("secret").toString();
		if (Login.getUsername(con).equals(username)) {
			if(Login.isRegistered(username)) {
				if (Login.getSecret(username).equals(secret)) {
					ServerCom.broadcastExceptClient(con, obj.toJSONString());
					obj.put("command", "ACTIVITY_BROADCAST");
					ServerCom.broadcastAll(obj);
					return true;
				}else {
					failObj.put("info", "incorrect secret of the user");
				}
			}else {
				ServerCom.broadcastExceptClient(con, obj.toJSONString());
				// if server cannot find the name in its database, proceed it to 
				obj.put("command", "ACTIVITY_BROADCAST");
				ServerCom.broadcastAll(obj);
			}
		}else {
			failObj.put("info", "attempt to send message with another user name");
		}

		con.writeMsg(failObj.toJSONString());
		con.closeCon();
		return false;
		//get whether is login anonymous
	}


}
