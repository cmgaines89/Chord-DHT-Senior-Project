/*
C. Gaines
Chord Project
*/

import java.util.*;
import java.io.*;
import java.net.*;

/*
Query class that provides an interface for users to do searches by querying a valid chord node
*/

public class Query
{
	private static InetSocketAddress localAddress;
	private static Hasher hasher;
	
	public static void main(String[] args)
	{
		hasher = new Hasher();
		
		if(args.length == 2)
		{
			localAddress = Hasher.createSocketAddress(args[0]+":"+args[1]); //parses the socket address from the argument
			if(localAddress == null) //on parsing fail
			{
				System.out.println("The address you are trying to contact cannot be found. Exiting Program...");
				System.exit(0);
			}
			
			String response = Hasher.sendRequest(localAddress, "KEEP");
			
			if(response == null || !response.equals("ALIVE"))
			{
				System.out.println("1The node you are trying to contact cannot be found. Exiting Program...");
				System.exit(0);
			}
			
			System.out.println("Connection to node "+localAddress.getAddress().toString()+", port "+localAddress.getPort()+" , position "+Hasher.hexIdAndPosition(localAddress)+".");
			
			//System stability check
			boolean pred = false;
			boolean succ = false;
			InetSocketAddress pred_addr = Hasher.requestAddress(localAddress, "YOUREPRE");
			InetSocketAddress succ_addr = Hasher.requestAddress(localAddress, "YOURSUCC");
			
			/*if(pred_addr == null || succ_addr == null)
			{
				System.out.println("2The node you are trying to contact has been disconnected. Exiting program...");
				System.exit(0);
			}
			
			if(pred_addr.equals(localAddress))
				pred = true;
				
			if(succ_addr.equals(localAddress))
				succ = true;
				*/
				
			//System is stable if the node has a valid predecessor and successor or neither
			while(pred^succ)
			{
				System.out.println("System Stabilizaing...");
				pred_addr = Hasher.requestAddress(localAddress, "YOURPRE");
				succ_addr = Hasher.requestAddress(localAddress, "YOURSUCC");
				
				if(pred_addr == null || succ_addr == null)
				{
					System.out.println("3The node you are trying to contact has been disconnected. Exiting program...");
					System.exit(0);
				}
				
				if(pred_addr.equals(localAddress))
					pred = true;
				else
					pred = false;
					
				if(succ_addr.equals(localAddress))
					succ = true;
				else
					succ = false;
					
				try
				{
					Thread.sleep(500);
				}
				catch(InterruptedException e)
				{}
				
			}
			
			//Get user input
			Scanner userinput = new Scanner(System.in);
			
			while (true)
			{
				System.out.println("Enter your search key, or type 'quit' to exit: ");
				String command = null;
				command = userinput.nextLine();
				
				if(command.startsWith("quit"))
				{
					System.exit(0);
				}
				
				else if(command.length() > 0)
				{ //search
				
					long hash = Hasher.hashString(command);
					System.out.println("The keys hash value is "+Long.toHexString(hash));
					InetSocketAddress result = Hasher.requestAddress(localAddress, "FINDSUCC_"+hash);
					
					if(result == null)
					{ //if send request fails the local node is disconnected
					
						System.out.println("4The node you are trying to contact has been disconnected. Exiting program...");
						System.exit(0);
					}
					
					System.out.println("Response from node: "+localAddress.getAddress().toString()+", port "+localAddress.getPort()+" , position "+Hasher.hexIdAndPosition(localAddress)+":");
					System.out.println("Node "+result.getAddress().toString()+", port "+result.getPort()+", position "+Hasher.hexIdAndPosition(result));
					
				}
			}
		}
		
		else
		{
			System.out.println("Invalid input. Exiting Program...");
		}
		
	}
	
} //END QUERY		
				
