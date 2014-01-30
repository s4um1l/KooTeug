import java.io.*;
import java.util.HashMap;
import java.io.IOException;
import java.util.List;
import java.util.ArrayList;

public class testClass {
	public static double[] initialValues;
	public static int numberOfNodes;

	public static int getTotalNodes(int NodeId) {

		File currentDirectory = new File(new File(".").getAbsolutePath());
		System.out.println("cd ---- " + currentDirectory.getName());
		File file = new File(currentDirectory + File.separator + "checkpoint"
				+ NodeId);
		String line;
		int line_count = 0;
		int count = 0;
		String[] tokens;

		try (BufferedReader br = new BufferedReader(new InputStreamReader(
				new FileInputStream(file)))) {
			while ((line = br.readLine()) != null) {
				tokens = line.split("\\s+");

				if (!line.isEmpty()) {
					line_count++;

					if (line_count == 3) {
						numberOfNodes = Integer.parseInt(tokens[0].trim());
					}

				}

			}

		} catch (IOException e) {
			e.printStackTrace();
		}
		return numberOfNodes;
	}

	public static double getBalance(int NodeId) {
		
		File currentDirectory = new File(new File(".").getAbsolutePath());
		File file = new File(currentDirectory + File.separator + "checkpoint"
				+ NodeId);
		String line;
		int line_count = 0;
		int count = 0;
		String[] tokens;
		Double balance = 0.00;

		try (BufferedReader br = new BufferedReader(new InputStreamReader(
				new FileInputStream(file)))) {
			while ((line = br.readLine()) != null) {
				tokens = line.split("\\s+");

				if (!line.isEmpty()) {
					line_count++;
					if (line_count == 1) {
						balance = Double.parseDouble(tokens[0].trim());
						System.out.println("balance is:::::: " + balance);
						line_count++;
					}

					else if (line_count == 3) {
						numberOfNodes = Integer.parseInt(tokens[0].trim());
					}

					else {
						line_count++;

					}
				}

			}

		} catch (IOException e) {
			e.printStackTrace();
		}
		return balance;
	}

	public static List<Integer> getClock(int NodeId) {
		
		File currentDirectory = new File(new File(".").getAbsolutePath());
		File file = new File(currentDirectory + File.separator + "checkpoint"
				+ NodeId);
		String line;
		int line_count = 0;
		List<Integer> clockVector = new ArrayList<Integer>();
		
		try (BufferedReader br = new BufferedReader(new InputStreamReader(
				new FileInputStream(file)))) {
			while ((line = br.readLine()) != null) {
				if (!line.isEmpty()) {
					line_count++;
					if (line_count == 4) {
						clockVector = retreiveList(line);
						/*System.out.print("clock: ");
						for(int i = 0; i < clockVector.size(); i++) {
							System.out.print(clockVector.get(i)+" ");
						}
						System.out.println();
						*/
					}

				}
			}

		} catch (IOException e) {
			e.printStackTrace();
		}
		return clockVector;
	}

	private static List<Integer> retreiveList(String line) {
		List<Integer> list = new ArrayList<>();
		String[] splits = line.split(",");
		for(int i = 0; i < splits.length; i++) {
			list.add(Integer.parseInt(splits[i].trim()));
		}
		return list;
	}


	private static boolean verifyBalance(Double total_balance) {

		double sum =0.0;		
		for(int i = 0; i < numberOfNodes; i++) {
			sum += initialValues[i];
		}		
		if (sum >= total_balance)
			return true;
		else
			return false;
	}

	private static boolean verifyClocks(List<List<Integer>> clockVectors) {

		boolean result = true;
		for(int i = 0 ; i < numberOfNodes; i++) {
			for(int j = i+1; j < numberOfNodes; j++) {
				result = verifyClock(clockVectors.get(i), clockVectors.get(j));
				if(result == false) {
					return false;
				}
			}
		}
		return true;
	}
	
	private static boolean verifyClock(List<Integer> clock1, List<Integer> clock2) {
		if(lessThan(clock1, clock2) || lessThan(clock2, clock1)) {
			return false;
		}
		return true;
	}
	
	private static boolean lessThan(List<Integer> clock1, List<Integer> clock2) {
		for(int i = 0; i < clock1.size(); i++) {
			if(clock2.get(i) < clock1.get(i)) {
				return false;
			}
		}
		return true;
	}
	
	public static void main(String[] args) throws IOException {

		System.out.println("*** Reading data from checkpoint files ***");
		Double total_balance = 0.00;

		int total_nodes = getTotalNodes(1);
		System.out.println("total nodes are: " + total_nodes);
		initialValues = new double[args.length];

		for(int i = 0; i < args.length; i++) {
			initialValues[i] = Double.parseDouble(args[i].trim());
		}


		List<List<Integer>> clockVectors = new ArrayList<>();
		for (int i = 0; i < total_nodes; i++) {
			List<Integer> clockVector = getClock(i);
			total_balance += getBalance(i);
			clockVectors.add(clockVector);
		}

		System.out.println("total balance is :" + total_balance);

		boolean result = verifyBalance(total_balance);
		boolean clockResult = verifyClocks(clockVectors);
		if(clockResult) {
			System.out.println("Clocks are consistent.");
		}
		if (result)
			System.out.println("Test is successful");
		// validate consistency function

	}

}
