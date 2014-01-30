1. COMPILING:
	
	Navigate to the src folder.
	Compile using the following command:
		javac /*/*.java

2. RUNNING:
	Navigate to the src folder.
	Run using the following command:
	java main.KooTouegDemo <node id> <initialBalance> <no. of application messages sent by each node> <path to config file>
	For ex: java main.KooTouegDemo 0 10 config.txt

NOTE: 
A) Please ensure that the config.txt is present in the src folder. Else create one having the following format:
<number of nodes>
<adjacency matrix separated by tabs>
<Node id><tab><hostname><tab><port number>
<Node id><tab><hostname><tab><port number>
		:
<Node id><tab><checkpoint timing><tab><recovery timing>
		:
		
Only one node should be allowed to fail.


B) The program should run sequentially in the order mentioned in the config file i.e. The program should be invoked on the machine with node is 0 then on the machine with node id 1 and so on..
