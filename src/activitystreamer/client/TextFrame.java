package activitystreamer.client;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.border.Border;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;

import java.net.UnknownHostException;
import java.io.IOException;

import activitystreamer.util.Settings;

@SuppressWarnings("serial")
public class TextFrame extends JFrame implements ActionListener {
	private static final Logger log = LogManager.getLogger();
	private JTextArea inputText;
	private JTextArea outputText;
	private JButton sendButton;
	private JButton disconnectButton;
	private JSONParser parser = new JSONParser();
	
	public TextFrame(){
		setTitle("ActivityStreamer Text I/O");
		JPanel mainPanel = new JPanel();
		mainPanel.setLayout(new GridLayout(1,2));
		JPanel inputPanel = new JPanel();
		JPanel outputPanel = new JPanel();
		inputPanel.setLayout(new BorderLayout());
		outputPanel.setLayout(new BorderLayout());
		Border lineBorder = BorderFactory.createTitledBorder(BorderFactory.createLineBorder(Color.lightGray),"JSON input, to send to server");
		inputPanel.setBorder(lineBorder);
		lineBorder = BorderFactory.createTitledBorder(BorderFactory.createLineBorder(Color.lightGray),"JSON output, received from server");
		outputPanel.setBorder(lineBorder);
		outputPanel.setName("Text output");
		
		inputText = new JTextArea();
		JScrollPane scrollPane = new JScrollPane(inputText);
		inputPanel.add(scrollPane,BorderLayout.CENTER);
		
		JPanel buttonGroup = new JPanel();
		sendButton = new JButton("Send");
		disconnectButton = new JButton("Disconnect");
		buttonGroup.add(sendButton);
		buttonGroup.add(disconnectButton);
		inputPanel.add(buttonGroup,BorderLayout.SOUTH);
		sendButton.addActionListener(this);
		disconnectButton.addActionListener(this);
		
		
		outputText = new JTextArea();
		scrollPane = new JScrollPane(outputText);
		outputPanel.add(scrollPane,BorderLayout.CENTER);
		
		mainPanel.add(inputPanel);
		mainPanel.add(outputPanel);
		add(mainPanel);
		
		setLocationRelativeTo(null); 
		setSize(1280,768);
		setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
		addWindowListener(new java.awt.event.WindowAdapter(){
			@Override
			public void windowClosing(java.awt.event.WindowEvent windowEvent) {
				ClientSkeleton.getInstance().disconnect();
				System.exit(0);
			}
		});
		setVisible(true);
	}

	public void setOutputText(final JSONObject obj){
		Gson gson = new GsonBuilder().setPrettyPrinting().create();
		JsonParser jp = new JsonParser();
		JsonElement je = jp.parse(obj.toJSONString());
		String prettyJsonString = gson.toJson(je);
		outputText.setText(prettyJsonString);
		outputText.revalidate();
		outputText.repaint();
	}
	
	@Override
	public void actionPerformed(ActionEvent e) {
		if(e.getSource()==sendButton){
			String msg = inputText.getText().trim().replaceAll("\r","").replaceAll("\n","").replaceAll("\t", "");
			System.out.println(msg);
			JSONObject obj,msgObj;
			
			try {
				obj = (JSONObject) parser.parse(msg);
				if (obj.get("command").equals("ACTIVITY_MESSAGE")) {
					ClientSkeleton.getInstance().sendActivityObject(obj);
				} else {
					log.info("Client is converting message to JSON ACTIVITY_MESSAGE");
					msgObj = new JSONObject();
					msgObj.put("command", "ACTIVITY_MESSAGE");
					msgObj.put("username", Settings.getUsername());
					msgObj.put("secret", Settings.getSecret());
					msgObj.put("activity", obj);
					ClientSkeleton.getInstance().sendActivityObject(msgObj);
				} 
			} catch (ParseException e1) {
			//log.error("invalid JSON object entered into input text field, data not sent");
			log.info("Client is converting message to JSON ACTIVITY_MESSAGE");
				obj = new JSONObject();
				obj.put("command", "ACTIVITY_MESSAGE");
				obj.put("username", Settings.getUsername());
				obj.put("secret", Settings.getSecret());
				msgObj = new JSONObject();
				msgObj.put ("message", msg);
				obj.put("activity", msgObj);
				ClientSkeleton.getInstance().sendActivityObject(obj);
			}
			
		} else if(e.getSource()==disconnectButton){
			ClientSkeleton.getInstance().disconnect();
		}
	}

}
