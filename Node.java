/*
C. Gaines
Chord Project
*/

import java.util.*;
import java.io.*;
import java.net.*;

/*
Implements the Node data structure and functionalities of the Chord Node
*/

public class Node
{

	private long localId;
	private InetSocketAddress localAddress;
	private InetSocketAddress predecessor;
	private HashMap<Integer, InetSocketAddress>finger;
	
	private FixFT fix_ft;
	private PortListener listener;
	private CheckPredecessor check_predecessor;
	private Stabilizer stabilizer;

	
	public Node (InetSocketAddress inet)
	{ //Node Constructor
	
		localAddress = inet;
		localId = Hasher.hashSocketAddress(localAddress);
		
		finger = new HashMap<Integer, InetSocketAddress>(); //initializes empty FT
		for(int i = 1; i <= 32; i++)
		{
			updateIthFinger(i, null);
		}
		
		predecessor = null; //initializes predecessor node
		
		listener = new PortListener(this); //initialize threads
		stabilizer = new Stabilizer(this);
		fix_ft = new FixFT(this);
		check_predecessor = new CheckPredecessor(this);
	}
	
	
	/*GETTERS*/
	
	public long getId()
	{
		return localId;
	}

	public InetSocketAddress getAddress()
	{
		return localAddress;
	}

	public InetSocketAddress getPredecessor()
	{
		return predecessor;
	}

	public InetSocketAddress getSuccessor()
	{
		if (finger != null && finger.size() > 0)
		{
			return finger.get(1);
		}
		return null;
	}
	
	
	/*NODE/CHORD RING EVENT FUNCTIONS*/
	
	public boolean join(InetSocketAddress contact)
	{ //Creates a chord ring or joins node to already created chord ring
	
		if(contact != null && !contact.equals(localAddress)) //if the contact is a different node, contact that node
		{
			InetSocketAddress successor = Hasher.requestAddress(contact, "FINDSUCC_" +localId);
			if(successor == null)
			{
				System.out.println("Cannot find the node you are trying to contact. Exiting Program...");
				return false;
			}
			
			updateIthFinger(1, successor);
		}
		
		listener.start(); //kickoff all threads after node joins the chord ring
		stabilizer.start();
		fix_ft.start();
		check_predecessor.start();
		
		return true;
	}
	
	
	
	/*SUCCESSOR/PREDECESSOR NODE FUNCTIONS*/
	
	private synchronized void setPredecessor(InetSocketAddress pre)
	{ //Sets predecessor node using a new value
		predecessor = pre;
	}

	
	private void fillSuccessor()
	{ //Fills successor node with candidiates from finger table, or uses the predecessor node
		InetSocketAddress successor = this.getSuccessor();
		if(successor == null || successor.equals(localAddress))
		{
			for(int i = 2; i <= 32; i++)
			{
				InetSocketAddress ithfinger = finger.get(i);
				if(ithfinger!= null && !ithfinger.equals(localAddress))
				{
					for(int j = i-1; j >= 1; j--)
					{
						updateIthFinger(j, ithfinger);
					}
					
					break;
				}
			}
		}
		
		successor = getSuccessor();
		if((successor == null || successor.equals(localAddress)) && predecessor!=null && !predecessor.equals(localAddress))
		{
			updateIthFinger(1, predecessor);
		}

	}
	
	private void deleteSuccessor()
	{ //Deletes successor node and all following nodes equal to the previous successor node
	
		InetSocketAddress successor = getSuccessor();
		
		if(successor == null) //if there is nothing to delete, simply return
			return;
			
		int i = 32; //finds the last existence of successor in the finger table
		for(i = 32; i > 0; i--)
		{
			InetSocketAddress ithF = finger.get(i);
			if(ithF != null && ithF.equals(successor))
				break;
		}
		
		for(int j = i; j >= 1; j--) //deletes last successor
			updateIthFinger(j, null);
			
		if(predecessor != null && predecessor.equals(successor)) //if predecessor node is successor, delete it
			setPredecessor(null);
			
		fillSuccessor(); //tries fills successor node
		successor = getSuccessor();

		if ((successor == null || successor.equals(successor)) && predecessor != null && !predecessor.equals(localAddress))
		{ //If the succesor node is still null or the local node and the predecessor node is another continue querying the
		 //predecessor node until it finds the local nodes new successor
		
			InetSocketAddress p = predecessor;
			InetSocketAddress p_pre = null;
			while (true)
			{
				p_pre = Hasher.requestAddress(p, "YOURPRE");
				if (p_pre == null)
					break;

				if (p_pre.equals(p) || p_pre.equals(localAddress)|| p_pre.equals(successor))
				{ //If p's predecessor is the chosen node, it is deleted, or if nothing is found in p or the local address
				 //p is the current nodes new sucessor
					
					break;
				}

				else //otherwise, keep querying
				{
					p = p_pre;
				}
			}

			// update successor
			updateIthFinger(1, p);
		}
	}
	
	public InetSocketAddress find_successor(long id)
	{ //Asks the current node to find the ID of the successor node

		InetSocketAddress ret = this.getSuccessor(); //initialize the return value as the nodes successor
		InetSocketAddress pre = find_predecessor(id); //get predecessor node

		if (!pre.equals(localAddress)) //if it is another node, get its successor
			ret = Hasher.requestAddress(pre, "YOURSUCC");

		if (ret == null) //if the return is null, set it as the local node
			ret = localAddress;

		return ret;
	}

	private InetSocketAddress find_predecessor(long findid)
	{
		InetSocketAddress n = this.localAddress;
		InetSocketAddress n_successor = this.getSuccessor();
		InetSocketAddress most_recently_alive = this.localAddress;
		long n_successor_relative_id = 0;
		
		if(n_successor != null)
			n_successor_relative_id = Hasher.computeRelativeId(Hasher.hashSocketAddress(n_successor), Hasher.hashSocketAddress(n));
		
		long findid_relative_id = Hasher.computeRelativeId(findid, Hasher.hashSocketAddress(n));

		while(!(findid_relative_id > 0 && findid_relative_id <= n_successor_relative_id))
		{
			InetSocketAddress pre_n = n; //temp to save the current node

			if(n.equals(this.localAddress)) //if the current node is the local node, find the closest
			{
				n = this.closest_preceding_finger(findid);
			}

			else //if the current node is remote, send a request to get the closest
			{
				InetSocketAddress result = Hasher.requestAddress(n, "CLOSEST_" + findid);

				if(result == null) //if no response, set n to the most recent node
				{
					n = most_recently_alive;
					n_successor = Hasher.requestAddress(n, "YOURSUCC");
					
					if(n_successor==null)
					{
						System.out.println("It's not possible.");
						return localAddress;
					}
					
					continue;
				}

				else if (result.equals(n)) //if n is the closest to itself, return itself
					return result;

				// else n's closest is other node "result"
				else
				{ //else if n's closest return the result	
					
					most_recently_alive = n; //set n as most receently alive		
					n_successor = Hasher.requestAddress(result, "YOURSUCC"); //get result from successor node	
					if (n_successor!=null) //if there is a response, it is the next node
					{
						n = result;
					}
					
					else
					{ //else n is successor
					
						n_successor = Hasher.requestAddress(n, "YOURSUCC");
					}
				}

				//compute the relative ID's for judgement in while loop
				n_successor_relative_id = Hasher.computeRelativeId(Hasher.hashSocketAddress(n_successor), Hasher.hashSocketAddress(n));
				findid_relative_id = Hasher.computeRelativeId(findid, Hasher.hashSocketAddress(n));
			}
			
			if (pre_n.equals(n))
				break;
		}
		
		return n;
	}

	
	public void clearPredecessor()
	{ //Clears the predecessor node
	
		setPredecessor(null);
	}
	
	public String notify(InetSocketAddress successor)
	{ //Notifies successor node that this node should be its predecessor
	
		if(successor!=null && !successor.equals(localAddress))
			return Hasher.sendRequest(successor, "IAMPRE_"+localAddress.getAddress().toString()+":"+localAddress.getPort());
		else
			return null;
	}
	
	public void notified (InetSocketAddress newpre)
	{ //Notifies another node and sets it as predecessor if it is
	
		if(predecessor == null || predecessor.equals(localAddress))
		{
			this.setPredecessor(newpre);
		}
		
		else
		{
			long oldpre_id = Hasher.hashSocketAddress(predecessor);
			long local_relative_id = Hasher.computeRelativeId(localId, oldpre_id);
			long newpre_relative_id = Hasher.computeRelativeId(Hasher.hashSocketAddress(newpre), oldpre_id);
			
			if(newpre_relative_id > 0 && newpre_relative_id < local_relative_id)
				this.setPredecessor(newpre);
		}
	}

		
	/*FINGER TABLE FUNCTIONS*/
	
	public synchronized void updateFingerTable(int i, InetSocketAddress inet)
	{ //Synchrnize all threads trying to modify the finger table. Updates the finger table based on params
	
		if(i > 0 && i <= 32) //index in [1-32], updates the ith Finger
			updateIthFinger(i, inet);
		
		else if(i == -1) //calling node wants to delete successor
			deleteSuccessor();
		
		else if(i == -2) //calling node wants to delete a finger in the finger table
			deleteCertainFinger(inet);
		
		else if(i == -3) //calling node wants to fill successor
			fillSuccessor();
	}
	
	private void updateIthFinger(int i, InetSocketAddress inet)
	{ //Updates ith finger in the finger table using a new value
	
		finger.put(i, inet);
		
		if(i == 1 && inet != null && !inet.equals(localAddress))
			notify(inet); //if the updated node is the successor, notify the new successor
	}
	
	
	private void deleteCertainFinger(InetSocketAddress f)
	{ //Deletes a node from the finger table (erases complete existence)
	
		for(int i = 32; i > 0; i--)
		{
			InetSocketAddress ithfinger = finger.get(i);
			if(ithfinger != null && ithfinger.equals(f))
				finger.put(i, null);
		}
	}
	
	
	public InetSocketAddress closest_preceding_finger(long findid)
	{ //Returms the closest finger to the preceeding node
	
		long findid_relative = Hasher.computeRelativeId(findid, localId);

		for(int i = 32; i > 0; i--) //checks the last item in the finger table
		{
			InetSocketAddress ith_finger = finger.get(i);
			if (ith_finger == null)
			{
				continue;
			}
			
			long ith_finger_id = Hasher.hashSocketAddress(ith_finger);
			long ith_finger_relative_id = Hasher.computeRelativeId(ith_finger_id, localId);

			if(ith_finger_relative_id > 0 && ith_finger_relative_id < findid_relative) //if the relative ID is the clocest check if alive
			{
				String response  = Hasher.sendRequest(ith_finger, "KEEP");

				if(response!=null &&  response.equals("ALIVE")) //if alive, return it
				{
					return ith_finger;
				}


				else
				{ //remove its existence from the finger table
				
					updateFingerTable(-2, ith_finger);
				}
			}
		}
		return localAddress;
	}
	
	
	/*PRINTABLES*/
	
	public void printNeighbors()
	{ //Prints node neighbors
	
		System.out.println("\nYou are listening on port "+localAddress.getPort()+"."+ "\nYour position is "+Hasher.hexIdAndPosition(localAddress)+".");
		InetSocketAddress successor = finger.get(1);
		
		//if predecessor and successor are not found
		if((predecessor == null || predecessor.equals(localAddress)) && (successor == null || successor.equals(localAddress)))
		{
			System.out.println("Your predecessor is yourself.");
			System.out.println("Your successor is yourself.");

		}
		
		else
		{ //if either successor or predecessor are found
		
			if(predecessor != null)
			{
				System.out.println("Your predecessor is node "+predecessor.getAddress().toString()+", "
						+ "port "+predecessor.getPort()+ ", position "+Hasher.hexIdAndPosition(predecessor)+".");
			}
			
			else
			{
				System.out.println("Your predecessor node is updating...");
			}

			if(successor != null)
			{
				System.out.println("Your successor is node "+successor.getAddress().toString()+", "
						+ "port "+successor.getPort()+ ", position "+Hasher.hexIdAndPosition(successor)+".");
			}
			
			else
			{
				System.out.println("Your successor node is updating...");
			}
		}
	}
	
	public void printDataStructure()
	{ //Prints finger table
	
		System.out.println("\n==============================================================");
		System.out.println("\nLOCAL:\t\t\t\t"+localAddress.toString()+"\t"+Hasher.hexIdAndPosition(localAddress));
		
		if(predecessor != null)
			System.out.println("\nPREDECESSOR:\t\t\t"+predecessor.toString()+"\t"+Hasher.hexIdAndPosition(predecessor));
		else 
			System.out.println("\nPREDECESSOR:\t\t\tNULL");
		
		System.out.println("\nFINGER TABLE:\n");
		for(int i = 1; i <= 32; i++)
		{
			long ithstart = Hasher.ithStart(Hasher.hashSocketAddress(localAddress),i);
			InetSocketAddress f = finger.get(i);
			StringBuilder sb = new StringBuilder();
			sb.append(i+"\t"+ Hasher.longTo8DigitHex(ithstart)+"\t\t");
			
			if(f!= null)
				sb.append(f.toString()+"\t"+Hasher.hexIdAndPosition(f));

			else 
				sb.append("NULL");
			
			System.out.println(sb.toString());
		}
		
		System.out.println("\n==============================================================\n");
	}
	
	
	/*EXECUTABLES*/
	
	public void stopAllThreads()
	{ //Stops all threads
	
		if (listener != null)
			listener.toDie();
		
		if (fix_ft != null)
			fix_ft.toDie();
			
		if (stabilizer != null)
			stabilizer.toDie();
			
		if (check_predecessor != null)
			check_predecessor.toDie();
	}
	
} //END NODE
