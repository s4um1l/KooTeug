package model;

import java.util.List;
import java.util.Vector;

/**
 * @author prarabdh
 * Class to hold the message structure. 
 * Has message string, message type, label, sender id, and clock value
 * (Add more fields if required).
 */
public class Message {
	
	private String message; 
	
	//Could be more OO if label is made a class.
	private int label;
	
	private MessageType messageType;
	
	private int senderId;
	
	private List<Integer> vectorClock;
	
	private int instance;
	
	public Message(String message, int label, MessageType messageType,
			int senderId, int checkpointInstance, List<Integer> vectorClock) {
		this.message = message;
		this.label = label;
		this.messageType = messageType;
		this.senderId = senderId;
		this.vectorClock = vectorClock;
		this.instance = checkpointInstance;
	}
	
	public String getMessage() {
		return message;
	}

	public int getLabel() {
		return label;
	}

	public MessageType getMessageType() {
		return messageType;
	}

	public int getSenderId() {
		return senderId;
	}

	public List<Integer> getVectorClock() {
		return vectorClock;
	}
	
	public int getInstance() {
		return instance;
	}

	public String toString() {
		String s = new String(message+" "+label+" "+messageType.toString()+
				" "+senderId+" "+instance);
		for(int i = 0; i < vectorClock.size(); i++) {
			s += " "+vectorClock.get(i);
		}
		return s;
	}

	public static Message toMessage(String message) {
		String[] s= message.split(" ");
		String mess = s[0];
		int label = Integer.parseInt(s[1].trim());
		MessageType type = MessageType.parseMessageType(s[2].trim());		
		int senderNodeId = Integer.parseInt(s[3].trim());
		int checkpointInstance = Integer.parseInt(s[4].trim());
		Vector<Integer> clock = new Vector<Integer>();
		for(int i = 5; i < s.length; i++) {
			clock.add(Integer.parseInt(s[i].trim()));
		}
		
		Message m = new Message(mess, label, type, senderNodeId, checkpointInstance, clock);		
		return m;
	}
}
