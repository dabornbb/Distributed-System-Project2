package activitystreamer.server;


import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import activitystreamer.util.Settings;


public class Connection extends Thread {
	private static final Logger log = LogManager.getLogger();
	private DataInputStream in;
	private DataOutputStream out;
	private BufferedReader inreader;
	private PrintWriter outwriter;
	private boolean open = false;
	private Socket socket;
	private boolean term=false;
	
	Connection(Socket socket) throws IOException{
		in = new DataInputStream(socket.getInputStream());
	    out = new DataOutputStream(socket.getOutputStream());
	    inreader = new BufferedReader( new InputStreamReader(in));
	    outwriter = new PrintWriter(out, true);
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
					}
					log.debug("connection closed to "+Settings.socketAddress(socket));
					Control.getInstance().removeChildConnectionList(this);
					in.close();
				}catch (IOException e) {
					log.error("connection "+Settings.socketAddress(socket)+" closed with exception: "+e);
					Control.getInstance().removeChildConnectionList(this);
				}
			}else if (Settings.getServerType().equals("m")){
				try {
					while(!term && (data = inreader.readLine())!=null){
						term=Control.getInstance().processMas(this,data);
					}
					log.debug("connection closed to "+Settings.socketAddress(socket));
					Control.getInstance().removeMasterConnectionList(this);
					in.close();
				}catch (IOException e) {
					log.error("connection "+Settings.socketAddress(socket)+" closed with exception: "+e);
					Control.getInstance().removeMasterConnectionList(this);
				}
					
			}else if (Settings.getServerType().equals("b")){
//				term=Control.getInstance().processBackUp(this,data);
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
