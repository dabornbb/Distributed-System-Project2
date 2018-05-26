package activitystreamer.server;

import java.util.*;

import org.json.simple.JSONObject;

import activitystreamer.util.Commands;

import java.util.ArrayList;
public class ServerList {
	public static ArrayList<ServerLoad> serverList= new ArrayList<ServerLoad>();
	private static int serverat = 0;


	public static ArrayList<ServerLoad> getServerList() {
		return serverList;
	}
	
	public static void update(String id, JSONObject obj,Connection con) {
		
		if (!isNewServer(id)) {
			serverList.get(serverat).setCon(con);
			int load,port;
			try {
				load = Integer.parseInt(obj.get("load").toString());
				serverList.get(serverat).setLoad(load);
			}catch(Exception e) {}
			try {
				port = Integer.parseInt(obj.get("port").toString());
				serverList.get(serverat).setPort(port);
			}catch(Exception e) {}
			try {
				String hostname = obj.get("hostname").toString();
				serverList.get(serverat).setHostname(hostname);
			}catch(Exception e) {}
		}else {
		}
		
	}

	public static void update(Connection con,JSONObject obj) {
		if (!isNewServer(con)) {
			try {
				String ID = obj.get("id").toString();
				serverList.get(serverat).setId(ID);
			}catch(Exception e) {}
			int load,port;
			try {
				load = Integer.parseInt(obj.get("load").toString());
				serverList.get(serverat).setLoad(load);
			}catch(Exception e) {}
			try {
				port = Integer.parseInt(obj.get("port").toString());
				serverList.get(serverat).setPort(port);
			}catch(Exception e) {}
			try {
				String hostname = obj.get("hostname").toString();
				serverList.get(serverat).setHostname(hostname);
			}catch(Exception e) {}
		}
	}
	
	//search by connection
	public static boolean isNewServer(Connection con) {
		serverat = 0;
		for (ServerLoad server : serverList) {
			if (server.getCon().equals(con))
				return false;
			serverat++;
		}
		return true;
	}
	
	// search by id
	public static boolean isNewServer(String id) {
		serverat = 0;
		
		for (ServerLoad server:serverList) {
			String serverId = server.getId();
			if (serverId!=null) {
				if(serverId.equals(id))
					return false;
			}
			serverat++;
		}
		return true;
	}
	
	
	public static void addServer(Connection con, String id,String hostname, int port) {
		serverList.add(new ServerLoad(con,id,hostname,port));
	}
	
	public static void addServer(Connection con) {
		serverList.add(new ServerLoad(con));
	}
	
	public static void deleteServer(Connection con) {
		if (!isNewServer(con)) serverList.remove(serverat);
	}

	public static ServerLoad redirectTo(String id) {
		// if the first server in list is the server itself, return null, else return the server
		if (serverList.get(0).getId().equals(id))
			return null;
		return serverList.get(0);
	}
	
	private static int getLoad(Connection con) {
		for (ServerLoad sl: serverList) {
			if (sl.getCon().equals(con)) {
				return sl.getLoad();
			}
		}
		return -1;
	}
	
	public static int length() {
		return serverList.size();
	}
	
	public static Connection getCon(String id) {
		for (ServerLoad sl:serverList) {
			if (sl.getId().equals(id))
				return sl.getCon();
		}
		return null;
	}
	
	// sort the list of child servers by lowest load to highest load , ArrayList <ServerLoad>
	public static void sortServerList_byLoad_fromLowest_toHighest() {
		Collections.sort(serverList);
		for (int i=0; i<serverList.size(); i++) {
			System.out.println(serverList.get(i).objToString());
		}
	}
}
