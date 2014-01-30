package model;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.FileHandler;
import java.util.logging.Level;
import java.util.logging.Logger;

import protocol.KooToueg;
import protocol.CheckpointingTimer;
import protocol.RecoveryTimer;
import util.NodeConfig;

import com.sun.nio.sctp.MessageInfo;
import com.sun.nio.sctp.SctpChannel;
import com.sun.nio.sctp.SctpServerChannel;

public class NodeImpl implements Node {
	
	private static Logger logger = Logger.getLogger(Node.class.getName());
	
	private FileHandler logFile;
	
	private boolean appBlocker;
	
	private int nodeId;
	
	private List<Integer> neighbours;	
	
	private int serverPort;
	
	private Map<Integer, SctpChannel> channels;
	
	private String configFilePath;
	
	private int timer;
	
	private int failTimer;
	
	private KooToueg kooToueg;
	
	private State current;
	
	private State tentativeCheckpoint;
	
	private boolean hasConnectionEstablished;
	
	private int messagesSent;
	
	private double balance;
	
	private boolean failed;
	
	public NodeImpl(int nodeId, int balance, String configFilePath, FileHandler logFile) {
		this.nodeId = nodeId;
		this.balance = balance;
		this.neighbours = new ArrayList<Integer>();
		this.channels = new HashMap<Integer, SctpChannel>();
		this.serverPort = 10000;
		this.configFilePath = configFilePath;
		this.appBlocker = false;
		this.failed = false;
		this.messagesSent = 0;
		this.logFile = logFile;
		logger.addHandler(logFile);
		logger.setUseParentHandlers(false);
		
	}
	
	public boolean getBlocker(){
		return appBlocker;
	}
	
	public int getNodeId() {
		return nodeId;
	}

	public List<Integer> getNeighbours() {
		return neighbours;
	}

	public int getServerPort() {
		return serverPort;
	}

	public Map<Integer, SctpChannel> getChannels() {
		return channels;
	}

	public String getConfigFilePath() {
		return configFilePath;
	}

	public KooToueg getKooToueg() {
		return kooToueg;
	}
	
    public synchronized boolean connectionEstablished() {
    	return hasConnectionEstablished;
    }

    public synchronized boolean failed() {
    	return failed;
    }

    private List<NodeConfig> configure() throws Exception {
		//reads configuration file and initializes
		List<NodeConfig> nodeConfigList = new ArrayList<NodeConfig>();
		File file = new File(configFilePath);
		BufferedReader br;
   		br = new BufferedReader(new FileReader(file));
   		String line = null;
   		int maxNodes = Integer.parseInt(br.readLine());
   		int count = 0;
   		while(count< nodeId) {
   			br.readLine();
   			count++;
   		}
   		line = br.readLine();
   		String[] row = line.split("\t");
   		for(int i = 0; i < row.length; i++) {
   			if(Integer.parseInt(row[i]) != 0) {
   				neighbours.add(i);
   			}
   		}
   		count++;
   		while(count < maxNodes) {
   			br.readLine();
   			count++;
   		}
   		count = 0;
   		while (count < maxNodes) {
   			line = br.readLine();
   			String[] idAddressPort = line.split("\t");
   			int id = Integer.parseInt(idAddressPort[0]);
   			String address = idAddressPort[1];
       		int port = Integer.parseInt(idAddressPort[2]);
   			nodeConfigList.add(new NodeConfig(id, address, port));
   			if(id == nodeId) {
   				serverPort = port;
   			}
   			count++;
   		}
   		while ((line = br.readLine()) != null) {
   			String[] idAddressPort = line.split("\t");
   			int id = Integer.parseInt(idAddressPort[0]);
   			int time = Integer.parseInt(idAddressPort[1]);
   			int failTime = Integer.parseInt(idAddressPort[2]);
   			if(id == nodeId) {
   				timer = time;
   				failTimer = failTime;
   			}
   		}
   		br.close();
   		return nodeConfigList;
	}
	
	private String processStringMessage(ByteBuffer byteBuffer) {
		byteBuffer.position(0);
		byteBuffer.limit(512);
		byte[] bufArr = new byte[byteBuffer.remaining()];
		byteBuffer.get(bufArr);
		return new String(bufArr);
    }
	
	private void startServer() throws IOException {
		//start a server
		SctpServerChannel serverSock = SctpServerChannel.open();
		InetSocketAddress serverAddr = new InetSocketAddress(serverPort);
		serverSock.bind(serverAddr);
		System.out.println("Service started at "+serverPort+" port.");
		while(channels.size() != neighbours.size()) {
			SctpChannel clientSock;
			clientSock = serverSock.accept();
			ByteBuffer byteBuffer;
			byteBuffer = ByteBuffer.allocate(512);
			MessageInfo messageInfo = clientSock.receive(byteBuffer,null,null);
			String strMessage = null;
			if(messageInfo != null) {
				strMessage = processStringMessage(byteBuffer);
			}
			int connectedTo = Integer.parseInt(strMessage.trim());
			if(strMessage != null)
				channels.put(connectedTo, clientSock);
			logger.log(Level.INFO, "Connected to "+connectedTo);
		}
	}
	
	private void connectToNeighbours(List<NodeConfig> nodeConfigList) throws Exception {
		for(NodeConfig nodeConfig: nodeConfigList) {
			int neighbourId = nodeConfig.getId();
			if(neighbourId >= nodeId) {
				break;
			}
			if(neighbours.contains(neighbourId)) {
				InetSocketAddress serverAddr;
				serverAddr = new InetSocketAddress(nodeConfig.getAddress(), 
						nodeConfig.getPort());
				SctpChannel socket = SctpChannel.open();
				socket.connect(serverAddr, 0, 0);
				logger.log(Level.INFO, "Connected to "+neighbourId);
				kooToueg.sendMessage(socket, nodeId+"");
				channels.put(neighbourId, socket);
			}
		}
	}
	
	@Override
	public void connect() {
		try {
			appBlocker = true;
			List<NodeConfig> nodeConfigList = configure();
			current = new State(nodeConfigList.size(), balance, nodeId);
			kooToueg = new KooToueg(channels, nodeConfigList.size(), nodeId, logFile);
			connectToNeighbours(nodeConfigList);
			startServer();
			logger.log(Level.INFO, "All connections established!");
			hasConnectionEstablished = true;
			resumeApp();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	@Override
	public void broadcastMessage(String message, MessageType type) {
		for(Map.Entry<Integer, SctpChannel> entry: channels.entrySet()) {
			try {
				kooToueg.sendMessage(entry.getValue(), message, 
						current.getLastLabelSent().get(entry.getKey()), 
						type, current);
			} catch (IOException e) {
				logger.log(Level.SEVERE, "Unable to broadcast message "+message+
						" to "+entry.getKey()+".The following exception was thrown: "+e);	
			}
		}
	}

	@Override
	public void initiateCheckPoint() {
		// If already in a check-pointing instance then return
		if(appBlocker) {
			return;
		}
		
		try {
			Action action = kooToueg.initiateCheckPointing(current);
			if(action!= null) {
				ActionType type = action.getActionType();
				switch(type) {
				case PERMANENT: 
					current.makePermanent();
					current.reset(current.getFirstLabelSent());
					current.reset(current.getLastLabelReceived());
					logger.log(Level.INFO, "Taking a permanent checkpoint");
					logger.info("Checkpoint: "+current);
					tentativeCheckpoint = null;
					
					return;
				case TENTATIVE: 
					tentativeCheckpoint = new State(current.getNumberOfNodes(),
							current.getBalance(), nodeId);
					current.saveTo(tentativeCheckpoint);
					current.reset(current.getFirstLabelSent());
					current.reset(current.getLastLabelReceived());
					// block application thread;
					appBlocker = true;
					logger.log(Level.INFO, "Taking a tentative checkpoint");
					return;
				case ABORT: tentativeCheckpoint = null; appBlocker = false;
				return;
				default: return;
				}
			}
			else appBlocker = false;
		} catch (IOException e) {
			logger.log(Level.SEVERE, "Unable to take a checkpoint: " +e);
		}
	}
	
	@Override
	public void failAndRecover() {
		if(kooToueg.getCheckpointInstance() != -1) {
			return;
		}
		kooToueg.setWillingToCheckpoint(false);
		synchronized (this) {
			appBlocker = true;
			failed = true;
		}
		kooToueg.setRecoveryInstance(nodeId);
		
		for(Map.Entry<Integer, SctpChannel> entry: channels.entrySet()) {
			try {
				kooToueg.sendRecoveryMessage(entry.getValue(), "failing", 
						current.getLastLabelSent().get(entry.getKey()), 
						MessageType.FAIL, current);
				kooToueg.getRecoveryCohorts().add(entry.getKey());
			} catch (IOException e) {
				logger.log(Level.SEVERE, "Unable to broadcast message \'failing\'"+
						" to "+entry.getKey()+".The following exception was thrown: "+e);	
			}
		}
	}
	
	@Override
	public void sendMessage(String message, int neighbour) {
		if(neighbours.contains(neighbour)) {
			try {
				messagesSent++;
				double amountDeducted = Double.parseDouble(message.split("_")[1].trim());
				current.deductAmount(amountDeducted);
				current.setFirstLabelSent(neighbour, messagesSent);
				current.getClock().set(nodeId, current.getClock().get(nodeId)+1);
				current.getTmpLabels().set(neighbour, messagesSent);
				kooToueg.sendMessage(channels.get(neighbour), message,
						messagesSent, 
						MessageType.APPLICATION, current);
				
			} catch (IOException e) {
				logger.log(Level.SEVERE, "Unable to send message "+message+
						" to "+neighbour+". The following exception was thrown: "+e);	
			}
		}
		else 
			logger.log(Level.SEVERE, neighbour+ "is not a neighbour of "+nodeId);
	}

	@Override
	public void run() {
		connect();
		try {
			Thread.sleep(1000);
		} catch (InterruptedException e1) {
			e1.printStackTrace();
		}
		CheckpointingTimer checkpointingTimer = new CheckpointingTimer(timer, this);
		Thread checkpointingTimerThread= new Thread(checkpointingTimer);
		checkpointingTimerThread.start();
		RecoveryTimer recoveryTimer = new RecoveryTimer(failTimer, this);
		Thread recoveryTimerThread = new Thread(recoveryTimer);
		recoveryTimerThread.start();
		boolean end = false;
		while(!end) {
			for(SctpChannel channel: channels.values()) {
				try {
					channel.configureBlocking(false);
					ByteBuffer byteBuffer;
					byteBuffer = ByteBuffer.allocate(512);
					MessageInfo messageInfo = channel.receive(byteBuffer,null,null);
					if(messageInfo != null) {
						String strMessage = processStringMessage(byteBuffer);
						Message message = Message.toMessage(strMessage);
						logger.log(Level.INFO, "Message Received: "+message);
						Action action = null;
						try {
							action = kooToueg.processMessage(message, current);
						} catch (IOException e) {
							e.printStackTrace();
						}
						if(action != null) {
							ActionType type = action.getActionType();
							switch(type) {
							case TRANSFER: current.addToBalance(action.getAmount()); 
							current.incrementReceive(message.getVectorClock());
							current.getLastLabelReceived().set(message.getSenderId(), 
									message.getLabel());
							break;
							case TENTATIVE: 
								tentativeCheckpoint = new State(current.getNumberOfNodes());
								current.saveTo(tentativeCheckpoint);
								logger.log(Level.INFO, "Taking a tentative checkpoint");
								//logger.info("tentative Checkpoint: "+tentativeCheckpoint);
								appBlocker = true;
								current.reset(current.getFirstLabelSent());
								current.reset(current.getLastLabelReceived());
								break;
							case PERMANENT: 
								logger.log(Level.INFO, "Taking a permanent checkpoint");
								//logger.info("tentative before perma: "+tentativeCheckpoint);
								tentativeCheckpoint.makePermanent();
								//logger.info("tentative after perma: "+tentativeCheckpoint);
								tentativeCheckpoint.saveTo(current);
								//logger.info("current after save: "+current);
								logger.info("Checkpoint: "+current);
								current.setLastLabelSent(current.getTmpLabels());
								tentativeCheckpoint = null;
								resumeApp();
								break;
							case ABORT: 
								tentativeCheckpoint = null;
								logger.log(Level.INFO, "Aborting checkpoint.");
								logger.info("Last Checkpoint: "+current.retreiveLastCheckpoint());
								resumeApp();
								break;
							case FAIL:
								if(kooToueg.getRecoveryInstance() != -1) {
									appBlocker = true;
								}
								break;
							case PERMANENTANDFAIL: 
								tentativeCheckpoint.makePermanent();
								tentativeCheckpoint.saveTo(current);
								current.setLastLabelSent(current.getTmpLabels());
								logger.log(Level.INFO, "Taking a permanent checkpoint "
										+ "and starting Failure Protocol");
								logger.info("Checkpoint: "+current);
								tentativeCheckpoint = null;
								kooToueg.checkAndAgree(current);
								if(kooToueg.getRecoveryInstance() == -1) {
									resumeApp();
								}
								break;
							case ABORTANDFAIL:
								tentativeCheckpoint = null;
								logger.log(Level.INFO, "Aborting checkpoint "
										+ "and starting Failure protocol");
								logger.info("Last Checkpoint: "+current.retreiveLastCheckpoint());
								kooToueg.checkAndAgree(current);
								if(kooToueg.getRecoveryInstance() == -1) {
									resumeApp();
								}
								break;
							case ROLLBACK: 
								logger.info("Roll backing to Last checkpoint");
								//logger.info("current before retreive: "+current);
								State temp = current.retreiveLastCheckpoint();
								temp.saveTo(current);
								//logger.info("current after retreive and save: "+current);
								logger.info("Checkpoint: "+current);
								current.reset(current.getLastLabelReceived());
								logger.info("Rollback successful");
								appBlocker = false;
								failed = false;
								synchronized (this) {
									notifyAll();
								}
								break;
							}
						}
						// else it is a protocol level message
					}
				} catch (Exception e) {
					//e.printStackTrace();
				}
			}
		}
		logger.log(Level.INFO, "Node terminated!");		
	}

	private void resumeApp() {
		logger.info("Resuming App...");
		appBlocker = false;
		synchronized (this) {
			notifyAll();
		}
	}

	public State getCurrent() {
		return current;
	}
}