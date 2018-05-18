package activitystreamer.server;

import org.json.simple.JSONObject;

import activitystreamer.util.Commands;

import java.util.ArrayList;
public class ServerList {
	public static ArrayList<ServerLoad> serverList= new ArrayList<ServerLoad>();
	private static int serverat = 0;
	public static int thisLoad;
	
	
	public static void update(String id, JSONObject obj) {
		
		if (!isNewServer(id)) {
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
	
	
	public static void addServer(Connection con, String hostname, int port) {
		ServerLoad server = new ServerLoad(con);
		server.setHostname(hostname);;
		server.setPort(port);
		serverList.add(server);
	}
	
	public static void addServer(Connection con) {
		serverList.add(new ServerLoad(con));
	}
	
	public static void deleteServer(Connection con) {
		if (!isNewServer(con)) serverList.remove(serverat);
	}

	public static ServerLoad redirectTo() {
		thisLoad = Login.onlineUsers.size();
		for (ServerLoad server: serverList) {
			System.out.println("[REDIRECTION]"+server.getHostname()+":"+server.getLoad());
			if ((thisLoad-2)>=server.getLoad()) {
				return server;
			}
			serverat++;
		}
		return null;
	}
}
