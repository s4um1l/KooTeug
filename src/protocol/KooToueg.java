package protocol;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.logging.FileHandler;
import java.util.logging.Level;
import java.util.logging.Logger;

import util.LogFormatter;
import model.Action;
import model.ActionType;
import model.Message;
import model.MessageType;
import model.Reply;
import model.State;

import com.sun.nio.sctp.MessageInfo;
import com.sun.nio.sctp.SctpChannel;

/**
 * @author prarabdh
 * Protocol class. The initiateCheckpoint method initiates the checkpoinint protocol.
 * The initiateRecovery method initiates the recovery protocol.
 */
public class KooToueg {
	
	private static Logger logger = Logger.getLogger(KooToueg.class.getName());
	
	private Map<Integer, SctpChannel> channels;
	
	private State previous;
	
	private int nodeId;
	
	private int checkpointInstance;
	
	private boolean willingToCheckpoint;
	
	private boolean willingToRecover;
	
	private static int NONE = -1;
	
	private List<Integer> cohorts;
	
	private int checkpointRequester;

	private List<Integer> agreedList;
	
	private List<Integer> rbokList;

	private int recoveryRequester;

	private int recoveryInstance;
	
	private List<Integer> recoveryCohorts;

	private List<Message> recoveryMessages;
	
	private List<Reply> replyList;
	
	private List<MessageType> rollBackReplyList;
	
	public KooToueg(Map<Integer, SctpChannel> channels, int numberOfNodes, int nodeId,
			FileHandler logFile) {
		this.channels = channels;
		this.nodeId = nodeId;
		checkpointInstance = NONE;
		recoveryInstance = NONE;
		willingToCheckpoint= true;
		willingToRecover = false;
		replyList = new ArrayList<Reply>();
		rollBackReplyList = new ArrayList<MessageType>();
		cohorts = new CopyOnWriteArrayList<Integer>();
		previous = new State(numberOfNodes);
		recoveryCohorts = new CopyOnWriteArrayList<Integer>();
		agreedList = new ArrayList<Integer>();
		rbokList = new ArrayList<Integer>();
		recoveryMessages = new CopyOnWriteArrayList<Message>();
		LogFormatter formatter = new LogFormatter();
		try {
			logFile.setFormatter(formatter);
		} catch (SecurityException e) {
			e.printStackTrace();
		}
		logger.addHandler(logFile);
		logger.setUseParentHandlers(false);
	}
	public List<Message> getRecoveryMessages() {
		return recoveryMessages;
	}
	
	public boolean getWillingToRecover() {
		return willingToRecover;
	}
	
	public Map<Integer, SctpChannel> getChannels() {
		return channels;
	}
	
	public int getCheckpointInstance() {
		return checkpointInstance;
	}

	public int getNodeId() {
		return nodeId;
	}

	public Action initiateCheckPointing(State current) throws IOException {
		if(!canTakeCheckpoint()) {
			return null;
		}
		Action action = null;
		logger.log(Level.INFO, "Node "+nodeId+" initiating checkpointing.");
		checkpointInstance = nodeId;
		for(Map.Entry<Integer, SctpChannel> entry: channels.entrySet()) {
			int label = current.getLastLabelReceived().get(entry.getKey());
			if(label != NONE) {
				cohorts.add(entry.getKey());
			}
		}
		if(cohorts.isEmpty()) {
			//take a permanent checkpoint
			action = new Action(ActionType.PERMANENT);
			reset();
			return action;
		}
		sendRequestMessage(cohorts, current);
		action = new Action(ActionType.TENTATIVE);
		replyList.clear();
		rollBackReplyList.clear();
		current.saveTo(previous);
		willingToCheckpoint = false;
		return action;
	}
	
	private boolean canTakeCheckpoint() {
		return willingToCheckpoint;
	}
	
	public List<Integer> getRecoveryCohorts() {
		return recoveryCohorts;
	}
	
	public int getRecoveryInstance() {
		return recoveryInstance;
	}
	
	public void setRecoveryInstance(int val) {
		recoveryInstance = val;
	}
	
	public Action processMessage(Message message, State current) throws IOException {

		Action action = null;
		int sender = message.getSenderId();
		MessageType type = message.getMessageType();
		int label = message.getLabel();
		
		switch(type) {
		case CHECKPOINT_REQUEST:
			if(checkpointInstance == message.getInstance()) {
				// Send Yes
				sendMessage(channels.get(sender), "I_have_already_taken_checkpoint_"
						+ "for_"+checkpointInstance, label, MessageType.YES, current);
			}
			else if(!willingToCheckpoint) {
				// This means that it is already in a checkpointingInstance or recoveryInstance
				sendMessage(channels.get(sender), "Already_in_a_checkpointing_Instance_for_"
						+checkpointInstance, 
						label, MessageType.NO, current);
			}
			else {
				int fls = current.getFirstLabelSent().get(sender);
				if(label >=	fls && fls > NONE ) {
					cohorts.clear();
					replyList.clear();
					// Take a tentative checkpoint
					action = new Action(ActionType.TENTATIVE);
					checkpointRequester = sender;
					// Set checkpointInstance to sender
					checkpointInstance = message.getInstance();
					
					for(Map.Entry<Integer, SctpChannel> entry: channels.entrySet()) {
						int lbl = current.getLastLabelReceived().get(entry.getKey());
						if(lbl != NONE && entry.getKey() != sender
								&& entry.getKey() != checkpointInstance) {
							cohorts.add(entry.getKey());
						}
					}
					sendRequestMessage(cohorts, current);
					current.saveTo(previous);
					if(cohorts.size() == 0) {
						//send yes to request sender
						int lbl = current.getLastLabelReceived().get(sender);
						sendMessage(channels.get(sender), "No_more_cohorts", lbl,  MessageType.YES,
								current);
					}
					willingToCheckpoint = false;
				}
				// else last checkpoint is already consistent
				// so send OK to sender
				else {
					sendMessage(channels.get(sender), 
							"dont_need_to_take_checkpoint_"+sender, message.getLabel(),
							MessageType.OK, current);
					reset();
				}
			}
			break;
			
		case APPLICATION:
			if(message.getMessage().contains("transfer")) {
				String[] transferAmount = message.getMessage().split("_");
				double amount = Double.parseDouble(transferAmount[1]);
				action = new Action(ActionType.TRANSFER, amount);
			}
			break;
			
		case ABORT:
			previous.saveTo(current);
			reset();
			action = new Action(ActionType.ABORT);
			if(willingToRecover) {
				action = new Action(ActionType.PERMANENTANDFAIL);
			}
			break;
			
		case YES:
			action = processReplyMessage(current, sender, type);
			break;
			
		case OK:
			action = processReplyMessage(current, sender, type);
			break;
			
		case NO:
			action = processReplyMessage(current, sender, type);
			break;
			
		case PERMANENT:
			if(checkpointInstance == message.getInstance()) {
				// take a permanent checkpoint
				action = new Action(ActionType.PERMANENT);
				if(!replyList.isEmpty()) {
					sendMessage(replyList, MessageType.PERMANENT, current);
				}
				reset();
				if(willingToRecover) {
					action = new Action(ActionType.PERMANENTANDFAIL);
				}
			}
			break;
			
		case FAIL:
			if(checkpointInstance != NONE) {
				willingToRecover = true;
				recoveryMessages.add(message);
			}
			else if(recoveryInstance == message.getInstance()) {
				// send RBAGREED to sender
				sendRecoveryMessage(channels.get(sender), "I_have_already_agreed_to_rollback_"
						+"for_"+recoveryInstance,
						current.getLastLabelSent().get(sender),
						MessageType.RBAGREED, current);
				action = new Action(ActionType.FAIL);
			}
			else {
				recoveryMessages.add(message);
				checkAndAgree(current);
				action = new Action(ActionType.FAIL);
			}
			break;
			
		case RBOK: 
			rbokList.add(sender);
			action = checkAndRollback(current, action, label);
			break;
		
		case RBAGREED: 
			agreedList.add(sender);
			action = checkAndRollback(current, action, label);
			break;
	
		case ROLLBACK: 
			if(recoveryInstance == message.getInstance()) {
				if(!agreedList.isEmpty()) {
					sendRecoveryMessage(agreedList, MessageType.ROLLBACK, current);
				}
				reset();
				//Roll back to previous permanent checkpoint
				action = new Action(ActionType.ROLLBACK);
				willingToRecover = false;
			}
			break;
		}
		return action;
	}
	
	private Action processReplyMessage(State current, int sender,
			MessageType type) throws IOException {
		Action action;
		replyList.add(new Reply(sender, type));
		action = checkAndPermanent(current);
		if(action != null && willingToRecover) {
			if(action.getActionType() == ActionType.ABORT) {
				action = new Action(ActionType.ABORTANDFAIL);
			}
			else if(action.getActionType() == ActionType.PERMANENT) {
				action = new Action(ActionType.PERMANENTANDFAIL);
			}
		}
		return action;
	}
	
	private Action checkAndPermanent(State current) throws IOException {
		Action action = null;
		
		if(replyList.size() == cohorts.size() && 
				(checkpointInstance != nodeId)) {
			int label = current.getLastLabelReceived().get(checkpointRequester);
			if(contains(replyList, MessageType.NO)) {
				//send No to requester
				sendMessage(channels.get(checkpointRequester), 
						"Received_No_from_some_cohorts_"+checkpointInstance, 
						label, MessageType.NO, current);
				// abort myself
				action = new Action(ActionType.ABORT);
			}
			else {
				//Send yes to its requester
				sendMessage(channels.get(checkpointRequester), 
					"Received_Yes_Ok_from_all_cohorts_"+checkpointInstance, 
					label, MessageType.YES, current);
			}
		}
		else if (replyList.size() == cohorts.size() && 
				(checkpointInstance == nodeId)) {
			if(contains(replyList, MessageType.NO)) {
				// send abort to all
				sendMessage(replyList, MessageType.ABORT, current);
				reset();
				//abort myself
				previous.saveTo(current);
				action = new Action(ActionType.ABORT);
			}
			else {//send permanent to all
				sendMessage(replyList, MessageType.PERMANENT, current);
				reset();
				//take a permanent checkpoint
				action = new Action(ActionType.PERMANENT);
			}
		}
		return action;
	}

	private Action checkAndRollback(State current, Action action, int label)
			throws IOException {
		if(((agreedList.size() + rbokList.size()) == recoveryCohorts.size())
		&& (recoveryInstance == nodeId)) {
			// recover
			if(!agreedList.isEmpty())
				sendRecoveryMessage(agreedList, MessageType.ROLLBACK, current);
			action = new Action(ActionType.ROLLBACK);
			reset();
			// 	send rollback message
		}
		else if(((agreedList.size() + rbokList.size()) == recoveryCohorts.size())
				&& (recoveryInstance != nodeId)) {
			//	Send RBAGREED to its requester
			sendRecoveryMessage(channels.get(recoveryRequester), 
					"Received_Replies_from_all_cohorts_"+recoveryInstance,
					+label, MessageType.RBAGREED, current);
		}
		return action;
	}

	public void checkAndAgree(State current) throws IOException {
		for(Message message : recoveryMessages) {
			int sender = message.getSenderId();
			if(recoveryInstance == message.getInstance()) {
				// send RBAGREED to sender
				sendRecoveryMessage(channels.get(sender), "I_have_already_agreed_to_rollback_"
						+"for_"+recoveryInstance,
						current.getLastLabelSent().get(sender),
						MessageType.RBAGREED, current);
			}
			else  {
				willingToCheckpoint = false;
				// if(received Replies from all neighbours - recoveryInstance - sender) 
				// send agreed to sender
				// check condition and agree to roll back
				State temp = current.retreiveLastCheckpoint();
				if(current.getLastLabelReceived().get(sender) > message.getLabel()) {
					recoveryRequester = sender;
					recoveryInstance = message.getInstance();
					// 	send fail to neighbours - sender - initiator with LLS
					for(Map.Entry<Integer, SctpChannel> entry: channels.entrySet()) {
						if(entry.getKey() != sender && entry.getKey() != message.getInstance()) {
							sendRecoveryMessage(entry.getValue(), 
									"sending_0_has_Failed_to_"+entry.getKey(), 
									temp.getLastLabelSent().get(entry.getKey()),
									MessageType.FAIL, current);
							recoveryCohorts.add(entry.getKey());
						}
					}
					
					if(recoveryCohorts.size() == 0) {
						// 	send RBAGREED to sender
						sendRecoveryMessage(channels.get(sender), "No_More_Recovery_Cohorts",
								current.getLastLabelSent().get(sender),
								MessageType.RBAGREED, current);
					}
				} else {
					// send RBOK to sender
					willingToRecover = false;
					sendRecoveryMessage(channels.get(sender), "I_dont_Need_to_Rollback", 
							current.getLastLabelReceived().get(sender),
							MessageType.RBOK, current);
					reset();
				}
			}
		}
		recoveryMessages.clear();
	}
	
	public void setWillingToCheckpoint(boolean val) {
		willingToCheckpoint = val;
	}
	
	public void reset() {
		checkpointInstance = NONE;
		willingToCheckpoint = true;
		cohorts.clear();
		replyList.clear();
		recoveryInstance = NONE;
		recoveryCohorts.clear();
		rbokList.clear();
		agreedList.clear();
	}
	
	private boolean contains(List<Reply> replyList, MessageType type) {
		for(Reply r: replyList) {
			if(r.getType() == type) {
				return true;
			}
		}
		return false;
	}
	
	private void sendRequestMessage(List<Integer> list, State current) throws IOException {
		for(Integer i : list) {
			int label = current.getLastLabelReceived().get(i);
			sendMessage(channels.get(i), "sending_to_"+i, label, MessageType.CHECKPOINT_REQUEST,
					current);
		}
	}
	
	private void sendMessage(List<Reply> list, MessageType type, State current) throws IOException {
		for(Reply r: list) {
			if(r.getType() == MessageType.YES) {
				int label = current.getLastLabelReceived().get(r.getSender());
				sendMessage(channels.get(r.getSender()), "sending_to_"+r.getSender(), 
						label, type, current);
			}
		}		
	}
   
	private void sendRecoveryMessage(List<Integer> list, MessageType type, State current) throws IOException {
		for(Integer i: list) {
			int label = current.getLastLabelReceived().get(i);
			sendRecoveryMessage(channels.get(i), "sending_to_"+i, label, type, current);
		}		
	}
   
	public void sendMessage(SctpChannel socket, String strMessage, 
			int label, MessageType type, State current) throws IOException {
		Message message = new Message(strMessage, 
				label, type, nodeId, checkpointInstance, current.getClock());
		String str = message.toString();
		sendMessage(socket, str);
	}

	public void sendRecoveryMessage(SctpChannel socket, String strMessage, 
			int label, MessageType type, State current) throws IOException {
		Message message = new Message(strMessage, 
				label, type, nodeId, recoveryInstance, current.getClock());
		String str = message.toString();
		sendMessage(socket, str);
	}
	
	public void sendMessage(SctpChannel socket, String str)
			throws IOException {
		MessageInfo messageInfo = MessageInfo.createOutgoing(null,0);
			
    	ByteBuffer sendBuffer = ByteBuffer.allocate(512);
		sendBuffer.clear();
		sendBuffer.put(str.getBytes());
		sendBuffer.flip();
		socket.send(sendBuffer, messageInfo);
		logger.info("Message sent: "+str);
	}
}