package activitystreamer.server;


import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.net.SocketException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import activitystreamer.util.Settings;
import activitystreamer.util.Commands;

public class Connection extends Thread {
	private static final Logger log = LogManager.getLogger();
	private DataInputStream in;
	private DataOutputStream out;
	private BufferedReader inreader;
	private PrintWriter outwriter;
	private boolean open = false;
	private Socket socket;
	private boolean term=false;
	//private BlockingQueue<MessageRecv> messageQueue;
	
	Connection(Socket socket) throws IOException{
		in = new DataInputStream(socket.getInputStream());
	    out = new DataOutputStream(socket.getOutputStream());
	    inreader = new BufferedReader( new InputStreamReader(in));
	    outwriter = new PrintWriter(out, true);
		//messageQueue = new LinkedBlockingQueue<MessageRecv>();
	    this.socket = socket;
	    open = true;
	    start();
	}
	
	/*
	 * returns true if the message was written, otherwise false
	 */
	public boolean writeMsg(String msg) {
		if(open){
			outwriter.println(msg);
			outwriter.flush();
			return true;
			
		}
		return false;
	}
	
	public void closeCon(){
		if(open){
			try {
				term=true;
//				inreader.close();
				out.close();
			} catch (IOException e) {
				// already closed?
				log.error("received exception closing the connection "+Settings.socketAddress(socket)+": "+e);
			}
		}
		log.info("reaches end");
	}
	
	
	public void run(){
			
			String data;

			if (Settings.getServerType().equals("c")) {
				try {
					while(!term && (data = inreader.readLine())!=null){
						term=Control.getInstance().processChild(this,data);
						if (Settings.getServerType().equals("b"))
							break;
					}
					if (!Settings.getServerType().equals("b")) {
						log.debug("connection closed to "+Settings.socketAddress(socket));
						Control.getInstance().removeChildConnectionList(this);
						in.close();
					}					
				//}catch (IOException e) {
				}catch (Exception e) {
					log.error("connection "+Settings.socketAddress(socket)+" closed with IOException: "+e);
					//Control.getInstance().removeChildConnectionList(this);
				}
			}else if (Settings.getServerType().equals("m")){
				try {
					while(!term && (data = inreader.readLine())!=null){
						term=Control.getInstance().processMas(this,data);
					}
					log.debug("connection closed to "+Settings.socketAddress(socket));
					Control.getInstance().removeMasterConnectionList(this);
					in.close();
				//}catch (IOException e) {
				}catch (Exception e) {
					log.error("connection "+Settings.socketAddress(socket)+" closed with exception: "+e);
					
					if (this.equals(MasCommands.getBackupCon())) {
						MasCommands.setHasBackup(false);
						if (ServerList.getServerList().size()>0) {
							log.info("promoting new backup...");
							ServerLoad sl = ServerList.getServerList().get(0);
							Connection newBackup = sl.getCon();
							MasCommands.setBackup(newBackup);
							Commands.sendPromotion(newBackup);
							MasCommands.setHasBackup(true);
							ServerList.deleteServer(newBackup);
						}
					} else {
						log.info("removing child server...");
						Control.getInstance().removeMasterConnectionList(this);
					}
				}
			}
			if (Settings.getServerType().equals("b")){
				try {
					while(!term && (data = inreader.readLine())!=null){
						term=Control.getInstance().processBackUp(this,data);
					}
				//}catch (IOException e) {
				}catch (Exception e) {
					log.error("connection "+Settings.socketAddress(socket)+" closed with exception: "+e);
					if (MasCommands.isMaster(this)) {
						MasCommands.setHasBackup(false);
						Settings.setServerType("m");
						log.info("I am now new MASTER...");
						while (ServerList.getServerList().size()>0) {
							log.info("contacting all child servers...");
							ServerLoad sl = ServerList.getServerList().get(0);
							MasCommands.contactChildServer(sl);
							ServerList.deleteServerByIndex(0);
						}
					} else {
						log.info("closing all users due to change of functions...");
					}
				}
			}

		open=false;
	}
	
	public Socket getSocket() {
		return socket;
	}
	
	public boolean isOpen() {
		return open;
	}
}