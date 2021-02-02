# Chord-DHT-Senior-Project
Chord DHT Project created for Senior Seminar Course.

The code was built using the Java programming language, using Java Runtime Edition (JRE) 11, Java Development Kit (JDK) 11 and Ubuntu 20.04 LTS distribution. The program uses several class files with multi-threading to demonstrate how the Chord DHT algorithm is implemented, as well as the Chord Ring geometry. 

•	The Chord class is the main class of the program, which offers UI to create a Chord node and join an existing Chord ring.

•	Query is a secondary main class which must be run concurrently with Chord.java upon joining the Chord ring. This class provides an interface for users to search for keys by querying a valid chord node via IP address and port number.

•	The Node class implements the Node data structure and the functionalities of the Chord node.

•	The Hasher class provides a myriad of services including hashing, computation, and network & address services

•	The Stabilizer class is a thread class which periodically asks the successor node for its predecessor and determines if the current node should be updated or deleted.

•	The CheckPredecessor class is a thread class which continuously checks the predecessor thread and periodically asks for the predecessor nodes “keep-alive” signal and will delete the predecessor node if it is dead.

•	The FixFT class is a thread class which accesses entries in the finger table and corrects them.

•	The PortListener class is a thread class that continuously listens to a port and asks the “talking” thread to process when a request us accepted.

•	The TalkToSocket class is a runnable thread class that processes requests accepted by listener nodes and writes their responses to the appropriate socket.

The Query class MUST be run concurrently with the Chord class in order for your query to work.
When running the percentages denote where your node is located on the Chord ring.

The zmake file will compile everything.
