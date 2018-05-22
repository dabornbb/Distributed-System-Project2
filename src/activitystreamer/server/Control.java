package activitystreamer.server;

import java.io.IOException;
import java.net.Socket;
import java.util.ArrayList;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import activitystreamer.util.*;

import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import org.json.simple.JSONObject;
import org.json.simple.JSONArray;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

public class Control extends Thread {
	private static final Logger log = LogManager.getLogger();
	private static boolean term=false;
	private static Listener listener;
	private JSONParser parser = new JSONParser();
	protected static Control control = null;
	private BlockingQueue<JSONObject> messageQueueSend;
	private BlockingQueue<JSONObject> messageQueueRecv;

	public static Control getInstance() {
		if(control==null){
			control=new Control();
		} 
		return control;
	}
	
	public Control() {
		Settings.setServerId();
		//messageQueueSend=new LinkedBlockingQueue<JSONObject>();
		messageQueueRecv=new LinkedBlockingQueue<JSONObject>();
		/*
		if (Settings.getRemoteHostname() == null) {
			Settings.setServerType("m");
		} else {
			Settings.setServerType("c");
		}
		*/
		// set the first child server as backup server
		if (!Settings.getServerType().equals("m")) {
			//if (!MasCommands.getHasBackup()) {
				//System.out.println("no back up yet");
				//Settings.setServerType("b");
			//} else {
				//System.out.println("already has backup");
				//Settings.setServerType("c");
			//}
		}
		
		if (Settings.getServerType().equals("c")) {
			System.out.println("[TYPE] Child Server");
		}else if (Settings.getServerType().equals("m")){
			System.out.println("[TYPE] Master Server");
		}else if (Settings.getServerType().equals("b")){
			System.out.println("[TYPE] Backup Server");
		} else {
			log.error("Invalid server type: "+Settings.getServerType());
		}
		
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
				ChildCommands.sendAuthenticate(con);
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
	public synchronized boolean processMas(Connection con,String msg){
		JSONObject obj;
		boolean term;
		try {
			obj = (JSONObject) parser.parse(msg);
			String cmd = obj.get("command").toString();
			System.out.println("[RECEIVED]" + msg);
			term = false;
			switch (cmd) {
				case "REGISTER":
					term = MasCommands.Register(con,obj);
					break;
				case "LOGIN":
					term = MasCommands.Login(con, obj);
					break;
				case "AUTHENTICATE":
					term = MasCommands.Authenticate(con, obj);
					break;
				case "BROADCAST_REQUEST":
					term = MasCommands.deliverList(con);
					break;
				case "UPDATE_LOAD":
					term = MasCommands.updateLoad(con,obj);
					break;
				default: 
					Commands.invalidMsg(con,"unknown commands");
					term = true;
					break;
			}
		} catch (ParseException e1) {
			log.error("invalid JSON object received at server, data is not processed");
			term = true;
		}
		return term;
	}

	public synchronized boolean processChild(Connection con, String msg) {
		log.debug("Calling child process");
		JSONObject obj;
		boolean term;
		try {
			obj = (JSONObject) parser.parse(msg);
			String cmd = obj.get("command").toString();
			log.debug(msg);
			term = false;
			switch (cmd) {
			case "AUTHENTICATION_SUCCESS":
				ChildCommands.setTimeInterval(obj);
				break;
			case "REGISTER":
				ChildCommands.client2MServer(con,obj);
				break;
			case "REGISTER_SUCCESS":
				ChildCommands.mServer2Client(obj);
				log.debug("term true due to register success");
				break;
			case "REGISTER_FAILED":
				ChildCommands.mServer2Client(obj);
				log.debug("term true due to register fail");
				break;
			case "LOGIN":
				ChildCommands.Login(con, obj);
				break;
			case "LOGIN_SUCCESS":
				ChildCommands.logUser(obj);
				break;
			case "LOGIN_FAILED":
				ChildCommands.mServer2Client(obj);
				log.debug("term true due to login fail");
				break;
			case "REDIRECT":
				ChildCommands.mServer2Client(obj);
				log.debug("term true due to redirection");
				break;
			case "LOGOUT":
				ChildCommands.logoutUser(con);
				log.debug("term true due to logout");
				break;
			case "PROMOTION":
				ChildCommands.promoteToNewRank(obj);
				break;
			default: 
				Commands.invalidMsg(con,"unknown commands");
				log.debug("term true due to invalid message");
				term = true;
				break;
			}
		} catch (ParseException e1) {
			log.error("invalid JSON object received at server, data is not processed");
			term = true;
		}
		return term;
	}
	public synchronized boolean processBackUp(Connection con,String msg){
		log.debug("Calling backup process");
		JSONObject obj;
		boolean term;
		try {
			obj = (JSONObject) parser.parse(msg);
			String cmd = obj.get("command").toString();
			System.out.println("[RECEIVED]" + msg);
			term = false;
			switch (cmd) {
				case "SYNC_DATA":
					log.info("data synced");
					break;
				default: 
					Commands.invalidMsg(con,"unknown commands");
					log.debug("term true due to invalid message");
					term = true;
					break;
			}
		} catch (ParseException e1) {
			log.error("invalid JSON object received at server, data is not processed");
			term = true;
		}
		return term;
	}
	/*
	 * The connection has been closed by the other party. -> ChildServer
	 */
	public synchronized void removeChildConnectionList(Connection con){
		log.info("removing connection from child lists "+con.getSocket().toString());
		if(!term) {
			
			//closed by client (login)
//			ChildCommands.logoutUser(con);
			
			// closed by client (not login)
			ChildCommands.connections.remove(con);
			
			//closed by server(no backup yet, tbc

		}
	}
	
	/*
	 * The connection has been closed by the other party. -> MasterServer
	 */
	public synchronized void removeMasterConnectionList(Connection con){
		log.info("removing connection "+con.getSocket().toString());
		if(!term) {
			MasCommands.deleteServer(con);
			
			// closed by backup server, tbc
		}
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
		ChildCommands.setMasterConnection(Control.getInstance().initiateConnection());
		//MasCommands.setMasterConnection(Control.getInstance().initiateConnection());
		int count = 0;
		while(!term){
					// do something with 5 second intervals in between
			try {
				Thread.sleep(Settings.getActivityInterval());
				if (Settings.getServerType().equals("c")) {
					Commands.updateLoad(ChildCommands.getMasCon(), Settings.getServerId(), ChildCommands.onlineLength());
					log.info("total client connections: "+ChildCommands.onlineLength());
				}
				if (Settings.getServerType().equals("m")) {
					log.info("total server connections (excluding backup): "+ServerList.length());
					if (count<6)
						count++;
					else {
						if (MasCommands.getHasBackup()) {
							Commands.backupMasterData();
							log.info("Backup interval: "+ (count*5) + " seconds");
						}
						count=0;
					}
				}				
			} catch (InterruptedException e) {
				log.info("received an interrupt, system is shutting down");
				break;
			}

				
		}
		
		
//		log.info("Thread terminated, closing connections");
		// clean up

		listener.setTerm(true);
	}
	
	
	public boolean doActivity(){
		return false;
	}
	
	public final void setTerm(boolean t){
		term=t;
	}
	
}
