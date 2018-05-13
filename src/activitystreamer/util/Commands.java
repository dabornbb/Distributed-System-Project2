package activitystreamer.util;

import activitystreamer.server.Connection;

import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import java.sql.Timestamp;

public class Commands {

	private static Timestamp time;
	private static JSONObject sendObj;
	//write message and automatically shut down connections
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
		time = new Timestamp(System.currentTimeMillis());
		sendObj = new JSONObject();
		sendObj.put("command", "LOGIN_SUCCESS");
		sendObj.put("info", "login anonymous");
		sendObj.put("logTime", time.getTime());
		con.writeMsg(sendObj.toJSONString());
	}
	
	
	public static void loginSuccess(Connection con,String username) {
		time = new Timestamp(System.currentTimeMillis());
		sendObj = new JSONObject();
		sendObj.put("command", "LOGIN_SUCCESS");
		sendObj.put("info","successfully login as user "+ username);
		sendObj.put("logTime", time.getTime());
		con.writeMsg(sendObj.toJSONString());
	}
	
	public static void loginFail(Connection con,String str) {
		sendObj = new JSONObject();
		sendObj.put("command", "LOGIN_FAILED");
		sendObj.put("info", str);
		con.writeMsg(sendObj.toJSONString());
		con.closeCon();
	}
	
	public static void registerSuccess(Connection con, String username,String secret) {
		sendObj = new JSONObject();
		sendObj.put("command", "REGISTER_SUCCESS");
		sendObj.put("info","logged in as user "+ username+", your secret is "+secret);
		con.writeMsg(sendObj.toJSONString());
	}
	
	public static void registerFail(Connection con, String username) {
		sendObj = new JSONObject();
		sendObj.put("command", "REGISTER_FAILED");
		sendObj.put("info", username+" is already registered in the system");
		con.writeMsg(sendObj.toJSONString());
	}

	public static void serverAnnounce(Connection con,String id,int load,String hostname,int port) {
		sendObj = new JSONObject();
		sendObj.put("command", "SERVER_ANNOUNCE");
		sendObj.put("id",id);
		sendObj.put("load", load);
		sendObj.put("hostname",hostname);
		sendObj.put("port", port);
		con.writeMsg(sendObj.toJSONString());
	}
	
	public static void sendAuthenticate(Connection con) {
		sendObj = new JSONObject();
		sendObj.put("command", "AUTHENTICATE");
		sendObj.put("secret", Settings.getSecret());
		con.writeMsg(sendObj.toJSONString());
	}
	
	public static void AuthenSuccess(Connection con) {
		sendObj = new JSONObject();
		time = new Timestamp(System.currentTimeMillis());
		sendObj.put("command", "AUTHENTICATION_SUCCESS");
		sendObj.put("time", time.getTime());
		con.writeMsg(sendObj.toJSONString());
	}

	public static void redirect(Connection con, String hostname, int port) {
		JSONObject sendObj= new JSONObject();
		sendObj.put("command", "REDIRECT");
		sendObj.put("hostname", hostname);
		sendObj.put("port", port);
		con.writeMsg(sendObj.toJSONString());
	}
}
