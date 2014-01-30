package util;

/**
 * @author prarabdh
 * A class to hold id, address and port numbers of all nodes.
 * Mainly used to establish connections between nodes (processes).
 */
public class NodeConfig {
	
	private int id;
	
	private String address;
	
	private int port;
	
	public NodeConfig(int id, String address, int port) {
		this.id = id;
		this.address = address;
		this.port = port;
	}

	public int getId() {
		return id;
	}

	public String getAddress() {
		return address;
	}

	public int getPort() {
		return port;
	}
}
