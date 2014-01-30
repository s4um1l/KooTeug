package model;

public class Reply {
	
	private int sender;
	
	private MessageType type;
	
	public Reply(int sender, MessageType type) {
		this.sender = sender;
		this.type = type;
	}
	
	public int getSender() {
		return sender;
	}
	
	public MessageType getType() {
		return type;
	}
}
