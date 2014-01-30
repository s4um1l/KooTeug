package model;

import java.util.Map;

import com.sun.nio.sctp.SctpChannel;

public interface Node extends Runnable{

	public void connect();

	public void sendMessage(String message, int neighbour);
	
	public void broadcastMessage(String message, MessageType type);
	
	public void initiateCheckPoint();

	public Map<Integer, SctpChannel> getChannels();

	void failAndRecover();

	public boolean connectionEstablished();
}
