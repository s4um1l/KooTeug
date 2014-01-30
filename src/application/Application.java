package application;

import java.util.logging.FileHandler;
import java.util.logging.Level;
import java.util.logging.Logger;

import model.NodeImpl;

public class Application implements Runnable {
    
	private static Logger logger = Logger.getLogger(Application.class.getName());
	
	private int Min;
    
    private int Max;
    
    private NodeImpl node;
    
    private int numberOfMessages;
    
    public Application(NodeImpl n, FileHandler logFile, int numMessages){
    	node = n;
    	Max = 500;
    	Min = 100;
    	numberOfMessages = numMessages;
    	logger.addHandler(logFile);
		logger.setUseParentHandlers(false);
    }
    
	@Override
	public void run() {
		synchronized (node) {
			while(!node.connectionEstablished()) {
				try {
					node.wait();
					
				} catch(InterruptedException e) {
					e.printStackTrace();
				}
			}
		}
		try {
			Thread.sleep(3000);
		} catch (InterruptedException e1) {
			e1.printStackTrace();
		}
		int number = 0;
		while(number < numberOfMessages){
			// thread sleeps for some time
			long time = Min + (int)(Math.random() * ((Max - Min) + 1));
			try {
				Thread.sleep(time);
				logger.log(Level.INFO, "App sleeping for "+time+" milliseconds...");
				synchronized (node) {
					while(node.getBlocker()) {
						logger.log(Level.INFO, "App blocked for Checkpointing or Recovery...");
						node.wait();
					}
					
 				}
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			//tries to send message
			
			double amount= (double)(Math.random()*(node.getCurrent().getBalance()));
			if(node.getCurrent().getBalance() - amount > 0) {
				int index = (int)(Math.random()*(node.getChannels().size()));
				int neighbour = node.getNeighbours().get(index);
				node.sendMessage("transfer_"+amount, neighbour);
				number++;
			}
		}
	}
}
