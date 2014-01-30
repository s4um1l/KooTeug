package main;

import java.io.IOException;
import java.util.logging.FileHandler;

import util.LogFormatter;
import application.Application;
import model.NodeImpl;

public class KooTouegDemo {
	public static void main(String[] args) throws IOException {
		if(args.length < 4) {
			System.out.println("Usage: java "
					+ "main.KooTouegDemo "
					+ "<Node Id> <InitialBalance> <Number of Application Messages>"
					+ " <Configuration File Path>");
			System.exit(-1);
		}
		int id = Integer.parseInt(args[0].trim());
		int initialBalance = Integer.parseInt(args[1].trim());
		int numMessages = Integer.parseInt(args[2].trim());
		String configFile = args[3];
		LogFormatter formatter = new LogFormatter();
		FileHandler logFile = new FileHandler("Node_"+id+".log");;
		try {
			logFile.setFormatter(formatter);
		} catch (SecurityException e) {
			e.printStackTrace();
		}
		
		NodeImpl node = new NodeImpl(id, initialBalance, configFile, logFile);
		Thread nodeThread = new Thread(node);
		nodeThread.start();
		Application app = new Application(node, logFile, numMessages);
		Thread appThread = new Thread(app);
		appThread.start();
	}
}
