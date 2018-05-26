package activitystreamer.util;

import activitystreamer.server.Connection;
import activitystreamer.server.ServerList;
import java.util.ArrayList;

import activitystreamer.server.*;
import activitystreamer.server.ServerLoad;
import activitystreamer.server.User;


import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.json.simple.JSONArray;

import java.sql.Timestamp;

public class Commands {


	private static JSONObject sendObj;
	//write message and automatically shut down connections
	
	// promotion
	public static void sendPromotion(Connection con) {
		sendObj = new JSONObject();
		sendObj.put("command", "PROMOTION");
		sendObj.put("secret", Settings.getSecret());
		sendObj.put("newRank", "backup");
		sendObj.put("message", "Congrats! You are now the backup server");
		con.writeMsg(sendObj.toJSONString());
	}
	public static void sendMasterBroadcast(Connection con) {
		sendObj = new JSONObject();
		sendObj.put("command", "NEW_MASTER");
		sendObj.put("secret", Settings.getSecret());
		sendObj.put("message", "I am now he new master server");
		sendObj.put("host name", Settings.getLocalHostname());
		sendObj.put("port", Settings.getLocalPort());
		con.writeMsg(sendObj.toJSONString());
	}

	// Data sync between master and backup servers

	public static void backupMasterData (){
		Connection con = MasCommands.getBackupCon();
		sendObj = new  JSONObject();
		JSONArray usr = new JSONArray();
		JSONArray svr = new JSONArray();
		
		ArrayList<User> tempu = MasCommands.getUserList();
		for (int i=0; i<tempu.size(); i++) {
			User u = tempu.get(i);
			usr.add(u.objToString());
		}
		ArrayList <ServerLoad> temps = ServerList.getServerList();
		for (int i=0; i<temps.size(); i++) {
			ServerLoad sl = temps.get(i);
			svr.add(sl.objToString());
		}

		sendObj.put("command", "SYNC_DATA");
		sendObj.put("secret", Settings.getSecret());
		sendObj.put("info", "Master syncs data with backup every 30s");
		sendObj.put("user list", usr);
		sendObj.put("server list", svr);
	}
	
	public static boolean invalidMsg(Connection con,String str) {
		sendObj = new JSONObject();
		sendObj.put("command", "INVALID_MESSAGE");
		sendObj.put("info", str);
		con.writeMsg(sendObj.toJSONString());
		con.closeCon();
		return false;
	}

	public static boolean authenFail(Connection con, String secret) {
		sendObj = new JSONObject();
		sendObj.put("command", "AUTHENTICATION_FAIL");
		sendObj.put("info", secret);
		con.writeMsg(sendObj.toJSONString());
		con.closeCon();
		return false;
	}
	
	public static void logoutSuccess(Connection con) {
		sendObj = new JSONObject();
		sendObj.put("command", "LOGOUT_SUCCESS");
		con.writeMsg(sendObj.toJSONString());
		con.closeCon();
	}

	public static void loginSuccess(Connection con) {
		sendObj = new JSONObject();
		sendObj.put("command", "LOGIN_SUCCESS");
		sendObj.put("info", "login anonymous");
		con.writeMsg(sendObj.toJSONString());
	}
	
	
	public static void loginSuccess(Connection con,JSONObject obj) {
		obj.put("command", "LOGIN_SUCCESS");
		obj.put("info","successfully login as user "+ obj.get("username"));
		con.writeMsg(obj.toJSONString());
	}
	
	public static void loginFail(Connection con,String str, JSONObject obj) {
		obj.put("command", "LOGIN_FAILED");
		obj.put("info", str);
		con.writeMsg(obj.toJSONString());
	}
	
	public static void registerSuccess(Connection con, JSONObject obj) {
		obj.put("command", "REGISTER_SUCCESS");
		con.writeMsg(obj.toJSONString());
	}
	
	public static void registerFail(Connection con, JSONObject obj) {
		obj.put("command", "REGISTER_FAILED");
		obj.put("info", obj.get("username").toString()+" is already registered in the system");
		con.writeMsg(obj.toJSONString());
	}
	
	
	public static void sendAuthenticate(Connection con,String id,String hostname, int portnum) {
		sendObj = new JSONObject();
		sendObj.put("command", "AUTHENTICATE");
		sendObj.put("secret", Settings.getSecret());
		sendObj.put("id", id);
		sendObj.put("hostname", hostname);
		sendObj.put("portnum", portnum);
		con.writeMsg(sendObj.toJSONString());
	}
	
	public static void AuthenSuccess(Connection con,Timestamp time) {
		sendObj = new JSONObject();
		sendObj.put("command", "AUTHENTICATION_SUCCESS");
		sendObj.put("time", time.getTime());
		con.writeMsg(sendObj.toJSONString());
	}

	public static void redirect(Connection con, String hostname, int port,JSONObject obj) {
		obj.put("command", "REDIRECT");
		obj.put("hostname", hostname);
		obj.put("port", port);
		con.writeMsg(obj.toJSONString());
	}
	
	public static void updateLoad(Connection con, String id, int load) {
		JSONObject sendObj= new JSONObject();
		sendObj.put("command", "UPDATE_LOAD");
		sendObj.put("id", id);
		sendObj.put("load", load);
		con.writeMsg(sendObj.toJSONString());
	}
}
