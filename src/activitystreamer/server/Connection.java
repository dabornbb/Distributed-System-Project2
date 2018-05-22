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


public class Connection extends Thread {
	private static final Logger log = LogManager.getLogger();
	private DataInputStream in;
	private DataOutputStream out;
	private BufferedReader inreader;
	private PrintWriter outwriter;
	private boolean open = false;
	private Socket socket;
	private boolean term=false;
	private BlockingQueue<MessageRecv> messageQueue;
	
	Connection(Socket socket) throws IOException{
		in = new DataInputStream(socket.getInputStream());
	    out = new DataOutputStream(socket.getOutputStream());
	    inreader = new BufferedReader( new InputStreamReader(in));
	    outwriter = new PrintWriter(out, true);
		messageQueue = new LinkedBlockingQueue<MessageRecv>();
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
			/*
			ClientMessageReader messageReader = new ClientMessageReader(inreader, messageQueue);
			messageReader.setName(this.getName() + "Reader");
			messageReader.start();
			*/
			/*
			System.out.println(Thread.currentThread().getName() 
					+ " - Processing client " + clientNum + "  messages");
			*/
			if (Settings.getServerType().equals("c")) {
				try {
					while(!term && (data = inreader.readLine())!=null){
						/*
						MessageRecv msg = messageQueue.take();
						data = msg.getMessage();
						if (!msg.isFromClient() && msg.getMessage().equals("exit"))
							break;
						*/
						term=Control.getInstance().processChild(this,data);
					}
					log.debug("connection closed to "+Settings.socketAddress(socket));
					Control.getInstance().removeChildConnectionList(this);
					in.close();
				}catch (IOException e) {
					log.error("connection "+Settings.socketAddress(socket)+" closed with IOException: "+e);
					Control.getInstance().removeChildConnectionList(this);
				}
				/*catch (InterruptedException e){
					log.error("connection "+Settings.socketAddress(socket)+" closed with InterruptedException: "+e);
					Control.getInstance().removeChildConnectionList(this);
				}*/
			}else if (Settings.getServerType().equals("m")){
				try {
					while(!term && (data = inreader.readLine())!=null){
						/*
						MessageRecv msg = messageQueue.take();
						data = msg.getMessage();
						if (!msg.isFromClient() && msg.getMessage().equals("exit"))
							break;
						*/
						term=Control.getInstance().processMas(this,data);
					}
					log.debug("connection closed to "+Settings.socketAddress(socket));
					Control.getInstance().removeMasterConnectionList(this);
					in.close();
				}catch (IOException e) {
					log.error("connection "+Settings.socketAddress(socket)+" closed with exception: "+e);
					Control.getInstance().removeMasterConnectionList(this);
				}
				/*catch (InterruptedException e){
					log.error("connection "+Settings.socketAddress(socket)+" closed with InterruptedException: "+e);
					Control.getInstance().removeChildConnectionList(this);
				}*/
					
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

class MessageSend {
	private static String msg;
	private static Connection con;
	public MessageSend (String m, Connection c){
		msg = m;
		con = c;
	}
	public static String getMessage(){
		return msg;
	}
	public static Connection getConnection(){
		return con;
	}
}

class MessageRecv {
		//True if the message comes from a client, false if it comes from a thread
	private boolean isFromClient;

	private String message;
	
	public MessageRecv(boolean isFromClient, String message) {
		//super();
		this.isFromClient = isFromClient;
		this.message = message;
	}
	
	public boolean isFromClient() {
		return isFromClient;
	}
	public String getMessage() {
		return message;
	}
}

class ClientMessageReader extends Thread {

	private BufferedReader reader; 
	private BlockingQueue<MessageRecv> messageQueue;
	
	public ClientMessageReader(BufferedReader reader, BlockingQueue<MessageRecv> messageQueue) {
		this.reader = reader;
		this.messageQueue = messageQueue;
	}
	
	@Override
	//This thread reads messages from the client's socket input stream
	public void run() {
		try {
			
			System.out.println(Thread.currentThread().getName() + " - Reading messages from client connection");
			
			String clientMsg = null;
			while ((clientMsg = reader.readLine()) != null) {
				System.out.println(Thread.currentThread().getName() + " - Message from client received: " + clientMsg);
				//place the message in the queue for the client connection thread to process
				MessageRecv msg = new MessageRecv(true, clientMsg);
				messageQueue.add(msg);
			}
			
			//If the end of the stream was reached, the client closed the connection
			//Put the exit message in the queue to allow the client connection thread to 
			//close the socket
			MessageRecv exit = new MessageRecv(false, "exit");
			messageQueue.add(exit);
			
		} catch (SocketException e) {
			//In some platforms like windows, when the end of stream is reached, instead
			//of returning null, the readLine method throws a SocketException, so 
			//do whatever you do when the while loop ends here as well
			MessageRecv exit = new MessageRecv(false, "exit");
			messageQueue.add(exit);		
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}