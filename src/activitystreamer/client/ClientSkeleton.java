package activitystreamer.client;

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import activitystreamer.util.Settings;

import java.net.UnknownHostException;

public class ClientSkeleton extends Thread {
	private static final Logger log = LogManager.getLogger();
	private static ClientSkeleton clientSolution;
	private TextFrame textFrame;
	private Socket clientSocket;
	private JSONParser parser = new JSONParser();
	private DataOutputStream outToServer;
	private DataInputStream inFromServer;
	private BufferedReader br;
	private boolean connected = false;
	private String info;
	public static ClientSkeleton getInstance(){
		if(clientSolution==null){
			clientSolution = new ClientSkeleton();
		}
		return clientSolution;
	}
	
	public ClientSkeleton(){
		System.out.println("[ACCESS] accessing clientSkeleton.ClientSkeleton...");
		initSocket();
		start();

	}
		
	
	@SuppressWarnings("unchecked")
	public void sendActivityObject(JSONObject activityObj) {
		System.out.println("[ACCESS] accessing clientSkeleton.sendActivityObject...");
		if (!connected) initSocket();
		try {
			String sentence = activityObj.toString();
			System.out.println("JSON object received: "+sentence);
			outToServer.writeBytes(sentence + '\n');
			connected = true;
		} catch (IOException e) {
			log.error("Client hit IOException at ClientSkeleton.sendActivityObject.Line.58");
			disconnect();
		}
		catch(NullPointerException e) {
			log.error("Client hit NullPointerException at ClientSkeleton.sendActivityObject.Line.62");
			disconnect();
		}
	}
	
	
	public void disconnect(){
		System.out.println("[ACCESS] accessing clientSkeleton.disconnect...");
		try {
			System.out.println("[DISCONNECT] trying to disconnect to server...");
			JSONObject logout = new JSONObject();
			logout.put("command", "LOGOUT");
			outToServer.writeBytes(logout.toJSONString()+'\n');
			inFromServer.close();
			outToServer.close();
			clientSocket.close();
			connected = false;
		}catch(IOException e) {}
		
	}
	
	
	public void run(){
		while(connected) {
			try {
				
				info = null;
				while ((info = br.readLine())!=null) {
					System.out.println("[ACCESS] accessing clientSkeleton.run with inread buffer...");
					JSONObject obj;
					obj = (JSONObject) parser.parse(info);
					System.out.println("[SYSTEM SAYS]"+info);
					String cmd = obj.get("command").toString();
					System.out.println("[ACCESS] accessing clientSkeleton.getCmd..."+cmd);
					switch (cmd) {
					case "REGISTER_SUCCESS":
						connected = false;
						break;						
					case "LOGIN_SUCCESS":
						textFrame = new TextFrame();
						textFrame.setOutputText(obj);
						connected = true;
						break;
					case "ACTIVITY_MESSAGE":
						JSONObject sendMsg = new JSONObject();
						String msg = obj.get("activity").toString();
						obj = (JSONObject) parser.parse(msg);
						textFrame.setOutputText(obj);
						connected = true;
						break;
					case "LOGOUT_SUCCESS":
						connected = false;
						break;
					case "REGISTER_FAIL":
						connected = false;
						System.out.println("Logged out because of register failure");
						break;
					case "REDIRECT":
						textFrame.setVisible(false);
						textFrame.dispose();
						System.out.println("System reaching redirection");
						redirection(obj);
						break;
					default:
						System.out.println("[CMD Default] SETTING CONNECTION TO FALSE");
						connected = false;
					}
					System.out.println("[FLAG] connected " +connected);
				}
				
	//			inFromServer.close();
				}catch (ParseException e) {
					System.out.println("[ACCESS] accessing clientSkeleton.runtimeParseException...");
				}
				catch(IOException e) {
					System.out.println("[ACCESS] accessing clientSkeleton.runtimeIOException...");}
				catch(NullPointerException e) {
					System.out.println("[ACCESS] accessing clientSkeleton.runtimeNullPointerException...");}
		}
		disconnect();
		System.exit(0);
	}
	
	
	private void initSocket() {
		System.out.println("[ACCESS] accessing clientSkeleton.Init...");
		try {
			clientSocket = new Socket(Settings.getRemoteHostname(),Settings.getRemotePort());
			outToServer = new DataOutputStream(clientSocket.getOutputStream());
			inFromServer = new DataInputStream(clientSocket.getInputStream());
			br = new BufferedReader(new InputStreamReader(inFromServer));
			System.out.println("Initiating socket to "+Settings.getRemoteHostname()+":"+Settings.getRemotePort());
			connected = true;
			//send username and secret to server in the type of JSONObject
			JSONObject sendObj = new JSONObject();
			String username,secret;
			if ((username = Settings.getUsername())=="anonymous") {
				
				//login anonymous
				sendObj.put("command", "LOGIN");
				sendObj.put("username",username);
			}else {
				if ((secret = Settings.getSecret())==null) {
					// if no secret, generate one then register
					secret = Settings.nextSecret();
					Settings.setSecret(secret);
					sendObj.put("command", "REGISTER");
				}else {
					
					// if there is secret, send login request
					sendObj.put("command","LOGIN");
					secret = Settings.getSecret();
				}
				sendObj.put("username", username);
				sendObj.put("secret", secret);
			}
			System.out.println(sendObj.toJSONString());
			outToServer.writeBytes(sendObj.toJSONString()+'\n');
		}catch (IOException e) {
			System.out.println("reaching ioexception"+e);
			}
		}

	private void redirection(JSONObject obj){
		System.out.println("[ACCESS] accessing clientSkeleton.Redirection...");
		disconnect();
		Settings.setRemoteHostname(obj.get("hostname").toString());
		Settings.setRemotePort(Integer.parseInt(obj.get("port").toString()));
		initSocket();
		
	}
}
