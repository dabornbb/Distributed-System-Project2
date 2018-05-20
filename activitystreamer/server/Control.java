package activitystreamer.server;

import java.io.IOException;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import activitystreamer.util.*;

import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

public class Control extends Thread {
	private static final Logger log = LogManager.getLogger();
	private static boolean term=false;
	private static Listener listener;
	private JSONParser parser = new JSONParser();
	private String id;
	//Arraylist that stores the information(id, load, ...) of other servers
	private static ArrayList<List<String>> servers;
	private int userat=0;
	protected static Control control = null;
	
	public static Control getInstance() {
		if(control==null){
			control=new Control();
		} 
		return control;
	}
	
	public Control() {
		Settings.setServerId();
		// start a listener
		try {
			listener = new Listener();
			System.out.println(Settings.getSecret());
			start();
		} catch (IOException e1) {
			log.fatal("failed to startup a listening thread: "+e1);
			System.exit(-1);
		}	
	}
	
	public Connection initiateConnection(){
		// make a connection to another server if remote hostname is supplied
		if(Settings.getRemoteHostname()!=null){
			try {
				Connection con = outgoingConnection(new Socket(Settings.getRemoteHostname(),Settings.getRemotePort()));
				ServerCom.sendAuthenticate(con);
				return con;
			} catch (IOException e) {
				log.error("failed to make connection to "+Settings.getRemoteHostname()+":"+Settings.getRemotePort()+" :"+e);
				System.exit(-1);
			}
		}
		return null;
	}
	
	/*
	 * Processing incoming messages from the connection.
	 * Return true if the connection should close.
	 */
	public synchronized boolean process(Connection con,String msg){
		JSONObject obj;
		boolean term;
		try {
			obj = (JSONObject) parser.parse(msg);
			String cmd = obj.get("command").toString();
			System.out.println("[RECEIVED]" + msg);
			switch (cmd) {
				case "REGISTER":
					term = !Login.registerUser(con, obj);
				case "LOGOUT": 
					Login.logoutUser(con);
					term = true;
					break;
				case "LOGIN":
					term = !Login.loginUser(con,obj);
					break;
                                case "RECONNECT_SUCCESS":
					term = !Login.loginUser(con,obj);
					break;
				case "ACTIVITY_MESSAGE":
					term = !Activity.actMsg(con,obj);
					break;
				case "SERVER_ANNOUNCE":
					term = !ServerCom.updateAnnouce(con, obj);
					break;
				case "AUTHENTICATE":
					term = !ServerCom.authenticate(con, obj);
					break;
				case "AUTHENTICATE_FAILED":
					ServerCom.authenFail(con);
					term = true;
					break;
				case "ACTIVITY_BROADCAST":
					term = !ServerCom.activityBroadcast(con, obj);
					break;
				case "LOCK_REQUEST":
					term = !ServerCom.broadcastLock(con, obj);
					break;
				case "LOCK_ALLOWED":
					term = !ServerCom.broadcastLock(con, obj);
					break;
				case "LOCK_DENIED":
					term = !ServerCom.broadcastLock(con, obj);
					break;
				default: 
					Commands.invalidMsg(con,"unknown commands");
					term = true;
					break;
			}
		} catch (ParseException e1) {
			log.error("invalid JSON object received at server, data is not processed");
			term = false;
		}
		return term;
	}

	
	
	/*
	 * The connection has been closed by the other party.
	 */
	public synchronized void connectionClosed(Connection con){
		getConnections();
		System.out.println("[Connection] removing connection "+con.getSocket().toString());
		if(!term) {
			Login.connections.remove(con);
			if (Login.onlineUsers.size()!=0) {
				int userInd = 0;
				for (OnlineUser user:Login.onlineUsers) {
					if (user.getSocket().equals(con.getSocket().toString())) break;
					userInd++;
				}
				if (userInd!=Login.onlineUsers.size()) Login.onlineUsers.remove(userInd);
			}
		}
		System.out.println("[Connection] total connections "+Login.connections.size());
		System.out.println("[Online] total online users "+Login.onlineUsers.size());
	}
	/*
	 * A new incoming connection has been established, and a reference is returned to it
	 */
	public synchronized Connection incomingConnection(Socket s) throws IOException{
		log.debug("incomming connection: "+Settings.socketAddress(s));
		Connection c = new Connection(s);		
		return c;
		
	}
	
	/*
	 * A new outgoing connection has been established, and a reference is returned to it
	 */
	public synchronized Connection outgoingConnection(Socket s) throws IOException{
		log.debug("outgoing connection: "+Settings.socketAddress(s));
		Connection c = new Connection(s);
		return c;
		
	}
	
	@Override
	public void run(){
		log.info("using activity interval of "+Settings.getActivityInterval()+" milliseconds");
		Connection con = Control.getInstance().initiateConnection();
		while(!term){
					// do something with 5 second intervals in between
				try {
					Thread.sleep(Settings.getActivityInterval());
//					System.out.println("[RUNNING] server list size "+ServerList.serverList.size());
					if (ServerList.serverList.size()!= 0 ) {
						ServerCom.sendAnnounce();
						System.out.println("[Connection] total connections "+Login.connections.size());
						System.out.println("[Online] total online users "+Login.onlineUsers.size());
					}
				} catch (InterruptedException e) {
					log.info("received an interrupt, system is shutting down");
					break;
				}
				if(!term){
	//				log.debug("doing activity");
					term=doActivity();
				}
					
			}
		
		
		log.info("closing "+Login.connections.size()+" connections");
		// clean up
		for(Connection connection : Login.connections){
			connection.closeCon();
		}
		listener.setTerm(true);
	}
	
	
	public boolean doActivity(){
		return false;
	}
	
	public final void setTerm(boolean t){
		term=t;
	}
	
	public final ArrayList<Connection> getConnections() {
		for (Connection connection : Login.connections) {
			System.out.println(connection.getSocket().toString());
		}
		return Login.connections;
	}
}
