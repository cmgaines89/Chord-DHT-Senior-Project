/*
Corinne Gaines
cosc 495 - Fall 2020
Chord Project
*/

import java.util.*;
import java.io.*;
import java.net.*;
import static java.lang.System.out;

/*
Main Class - Offers UI to create a Chord Node and join an existing ring
*/

public class Chord
{
	private static Node node;
	private static InetSocketAddress contact;
	private static Hasher hasher;

	public static void main(String[] args) throws SocketException
	{
		hasher = new Hasher();
		
		String host_name = null;
		String sys_ip = null;
  
		try
		{
			InetAddress ilocalhost = InetAddress.getLocalHost();
			host_name = ilocalhost.getHostName();
			sys_ip = ilocalhost.getHostAddress();
		}
		
		catch(Exception e)
		{
			host_name = "Cannot get Host Name";
			sys_ip = "Cannot get System IP Address";
		}	
		
		node = new Node(Hasher.createSocketAddress(sys_ip+":"+args[0])); //create new node
		
		if(args.length == 1)
		{
			contact = node.getAddress();
		}
		
		else if(args.length == 3)
		{
			contact = Hasher.createSocketAddress(args[1]+":"+args[2]);
			
			if(contact == null)
			{
				System.out.println("Cannot find socket address. Exiting Program...");
				return;
			}
		}
		
		else
		{
			System.out.println("Incorrect Input. Exiting program...");
			System.exit(0);
		}
		
		boolean successful_join = node.join(contact);
		
		if(!successful_join)
		{
			System.out.println("Cannot connect to the node you are trying to contact. Exiting Program...");
			System.exit(0);
		}
		
		System.out.println("Joining the Chord Ring...");
		System.out.println("Host Name: "+host_name);
		System.out.println("System IP Address: "+sys_ip);
		node.printNeighbors();
		
		Scanner input = new Scanner(System.in);
		while (true)
		{
			System.out.println("\nType 'info' to check this node's data or type 'quit' to leave the Chord Ring.");
			String command = null;
			command = input.next();
		
			if(command.startsWith("quit"))
			{
				node.stopAllThreads();
				System.out.println("Leaving the Chord Ring...");
				System.exit(0);
			}
		
			else if(command.startsWith("info"))
			{
			node.printDataStructure();
			}
		}
	
	}

} //END CHORD
