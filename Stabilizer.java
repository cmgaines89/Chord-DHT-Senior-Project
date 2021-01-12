/*
Corinne Gaines
COSC 495 - Fall 2020
Chord Project
*/

import java.util.*;
import java.io.*;
import java.net.*;

/*Stabilizer thread that periodically asks yje successor node for its predecessor and determines if the current node should be updated or deleted*/

public class Stabilizer extends Thread
{
	private Node local;
	private boolean alive;
	
	public Stabilizer(Node _local)
	{
		local = _local;
		alive = true;
	}
	
	@Override
	public void run()
	{
		while (alive)
		{
			InetSocketAddress successor = local.getSuccessor();
			if(successor == null || successor.equals(local.getAddress()))
			{
				local.updateFingerTable(-3, null); 
			}
			
			successor = local.getSuccessor();
			
			if(successor != null && !successor.equals(local.getAddress()))
			{
				InetSocketAddress x = Hasher.requestAddress(successor, "YOURPRE"); //get successor nodes predecessor
				
				if(x == null) //if theres a bad connection with the successor delete it
				{
					local.updateFingerTable(-1, null);
				}
				
				else if(!x.equals(successor)) //if the successors predecessor is not itself
				{
					long local_id = Hasher.hashSocketAddress(local.getAddress());
					long successor_relative_id = Hasher.computeRelativeId(Hasher.hashSocketAddress(successor), local_id);
					long x_relative_id = Hasher.computeRelativeId(Hasher.hashSocketAddress(x),local_id);
					
					if (x_relative_id>0 && x_relative_id < successor_relative_id)
					{
						local.updateFingerTable(1,x);
					}
				}
				
				else //if the successor nodes predecessor is itself then notify the successor node
				{
					local.notify(successor);
				}
			}

			try
			{
				Thread.sleep(60);
			} 
			
			catch (InterruptedException e)
			{
				e.printStackTrace();
			}
		}

	}

	public void toDie()
	{
		alive = false;
	}

}//END STABILIZER
