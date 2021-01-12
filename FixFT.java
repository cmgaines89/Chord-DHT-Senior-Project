/*
Corinne Gaines
COSC 495 - Fall 2020
Chord Project
*/

import java.util.*;
import java.io.*;
import java.net.*;

/*Thread that accesses random entry in the finger table and corrects it*/

public class FixFT extends Thread
{
	private Node local;
	Random random;
	boolean alive;
	
	public FixFT (Node node)
	{
		local = node;
		alive = true;
		random = new Random();
	}
	
	@Override
	public void run()
	{
		while(alive)
		{
			int i = random.nextInt(31) + 2;
			InetSocketAddress ithFinger = local.find_successor(Hasher.ithStart(local.getId(), i));
			local.updateFingerTable(i, ithFinger);
			
			try
			{
				Thread.sleep(500);
			}
			
			catch(InterruptedException e)
			{
				e.printStackTrace();
			}
		}
	}
	
	public void toDie()
	{
		alive = false;
	}
	
}//END FIXFT
