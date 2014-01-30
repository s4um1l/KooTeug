package model;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;

public class State {
	
	public static final int BOTTOM = -1;
	
	private List<Integer> lastLabelReceived;
	
	private List<Integer> firstLabelSent;
	
	private List<Integer> lastLabelSent;
	
	private int numberOfNodes;
	
	private double balance;
	
	private String checkpointFilePath;
	
	private List<Integer> clock;
	
	private List<Integer> tmpLabels;
	
	private int nodeId;
	
	public State(int N, double initialBalance, int id) {
		numberOfNodes = N;
		balance = initialBalance;
		lastLabelReceived = new ArrayList<Integer>();
		firstLabelSent = new ArrayList<Integer>();
		lastLabelSent = new ArrayList<Integer>();
		tmpLabels= new ArrayList<Integer>();
		this.nodeId = id;
		checkpointFilePath = "checkpoint"+nodeId;
		clock = new ArrayList<Integer>();
		clock.clear();
		for(int i = 0; i < numberOfNodes; i++) {
			clock.add(0);
		}
		reset(lastLabelReceived);
		reset(firstLabelSent);
		reset(lastLabelSent);
		reset(tmpLabels);
	}
	
	public State(int numberOfNodes) {
		balance = 0;
		nodeId = 0;
		this.numberOfNodes = numberOfNodes;
		lastLabelReceived = new ArrayList<Integer>();
		firstLabelSent = new ArrayList<Integer>();
		lastLabelSent = new ArrayList<Integer>();
		tmpLabels= new ArrayList<Integer>();
		checkpointFilePath = "checkpoint"+nodeId;
		clock = new ArrayList<Integer>();
		clock.clear();
		for(int i = 0; i < numberOfNodes; i++) {
			clock.add(0);
		}
		reset(lastLabelReceived);
		reset(firstLabelSent);
		reset(lastLabelSent);
		reset(tmpLabels);
	}
	
	public int getNodeId() {
		return nodeId;
	}

	public void setNodeId(int nodeId) {
		this.nodeId = nodeId;
	}

	public void setLastLabelReceived(List<Integer> lastLabelReceived) {
		this.lastLabelReceived = lastLabelReceived;
	}


	public void setFirstLabelSent(List<Integer> firstLabelSent) {
		this.firstLabelSent = firstLabelSent;
	}

	public void setLastLabelSent(List<Integer> lastLabelSent) {
		int index = 0;
		for(Integer i: lastLabelSent) {
			this.lastLabelSent.set(index, i);
			index++;
		}
	}

	public void setBalance(double balance) {
		this.balance = balance;
	}

	public int getNumberOfNodes() {
		return numberOfNodes;
	}
	
	public void abort() {
		balance = 0;
		clock.clear();for(int i = 0; i < numberOfNodes; i++) {
			clock.add(0);
		}
		reset(lastLabelReceived);
		reset(firstLabelSent);
		reset(lastLabelSent);
	}
	
	public List<Integer> getLastLabelReceived() {
		return lastLabelReceived;
	}
	
	public List<Integer> getTmpLabels() {
		return tmpLabels;
	}
	
	public List<Integer> getClock() {
		return clock;
	}
    
	public void setFirstLabelSent(int neighbour, int messageSent){
    	if (firstLabelSent.get(neighbour)==BOTTOM){
    		firstLabelSent.set(neighbour, messageSent);
    	}
    }
	
	public void saveTo(State s) {
		if(s == null) {
			s = new State(numberOfNodes, balance, nodeId);
		}
		for(int i = 0; i < firstLabelSent.size(); i++) {
			s.getFirstLabelSent().set(i, firstLabelSent.get(i));
		}
		for(int i = 0; i < lastLabelReceived.size(); i++) {
			s.getLastLabelReceived().set(i, lastLabelReceived.get(i));
		}
		for(int i = 0; i < lastLabelSent.size(); i++) {
			s.getLastLabelSent().set(i, lastLabelSent.get(i));
		}
		for(int i = 0; i < tmpLabels.size(); i++) {
			s.getTmpLabels().set(i, tmpLabels.get(i));
		}
		
		for(int i = 0; i < clock.size(); i++) {
			s.getClock().set(i, clock.get(i));
		}
		s.setBalance(balance);
		s.setNodeId(nodeId);
		s.setNodes(numberOfNodes);
		s.setCheckpointFilePath("checkpoint"+nodeId);
	}
	
	public void setCheckpointFilePath(String checkpointFilePath2) {
		this.checkpointFilePath = checkpointFilePath2;
	}

	public List<Integer> getFirstLabelSent() {
		return firstLabelSent;
	}

	public List<Integer> getLastLabelSent() {
		return lastLabelSent;
	}

	public void reset(List<Integer> list) {
		list.clear();
		for(int i = 0; i < numberOfNodes; i++) {
			list.add(i, BOTTOM);
		}
	}
	
	public void addToBalance(double amount) {
		balance += amount;
	}

	public void deductAmount(double amountDeducted) {
		balance -= amountDeducted;
	}

	public void makePermanent() {
		File file = new File(checkpointFilePath);
		PrintWriter writer;
   		try {
			writer = new PrintWriter(file);
			String str = toString();
			writer.write(str);
			writer.close();
   		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
	}

	public String toString() {
		String str = balance+"\n";
		str += nodeId+"\n";
		str += numberOfNodes+"\n";
		str = writeList(str, clock);
		str = writeList(str, lastLabelReceived);
		str = writeList(str, firstLabelSent);
		for(int i = 0; i< tmpLabels.size(); i++) {
			lastLabelSent.set(i, tmpLabels.get(i));
		}
		str = writeList(str, lastLabelSent);
		
		return str;
	}
	
	public State retreiveLastCheckpoint() {
		State state = new State(numberOfNodes);
		File file = new File(checkpointFilePath);
		try{
			BufferedReader reader = new BufferedReader(new FileReader(file));
			String line = "";
			int count = 0;
			while((line = reader.readLine()) != null) {
				if(count == 0) {
					state.setBalance(Double.parseDouble(line.trim()));
				}
				else if(count == 1) {
					state.setNodeId(Integer.parseInt(line.trim()));
				}
				else if(count == 2) {
					state.setNodes(Integer.parseInt(line.trim()));
				}
				else if(count == 3) {
					List<Integer> list = retreiveList(line);
					state.setClock(list);
				}
				else if(count == 4) {
					List<Integer> list = retreiveList(line);
					state.setLastLabelReceived(list);
				}
				else if(count == 5) {
					List<Integer> list = retreiveList(line);
					state.setFirstLabelSent(list);
				}
				else if(count == 6) {
					List<Integer> list = retreiveList(line);
					state.setLastLabelSent(list);
				}
				count++;
			}
			reader.close();
		
		} catch(FileNotFoundException e) {
			return null;
		} catch (IOException e) {
			e.printStackTrace();
		}
		return state;
	}

	private List<Integer> retreiveList(String line) {
		List<Integer> list = new ArrayList<>();
		String[] splits = line.split(",");
		for(int i = 0; i < splits.length; i++) {
			list.add(Integer.parseInt(splits[i].trim()));
		}
		return list;
	}

	public String writeList(String str, List<Integer> list) {
		for(int i = 0; i < list.size(); i++) {
			str += list.get(i)+",";
		}
		str +="\n";
		return str;
	}
	
	public void incrementReceive(List<Integer> vClock) {
		for(int i=0;i<numberOfNodes;i++){
			clock.set(i,Math.max(clock.get(i), vClock.get(i)));
		}
		clock.set(nodeId, clock.get(nodeId)+1);
	}

	public void setClock(List<Integer> clock2) {
		clock = clock2;
	}

	public void setNodes(int size) {
		numberOfNodes = size;
	}

	public double getBalance() {
		return balance;
	}
}
