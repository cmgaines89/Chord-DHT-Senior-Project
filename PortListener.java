/*
Corinne Gaines
COSC 495 - Fall 2020
Chord Project
*/

import java.util.*;
import java.io.*;
import java.net.*;

/*Port listener thread that contiuously listens to a port and asks the "talking" thread to process when a request is accepted*/

public class PortListener extends Thread
{
	private Node local;
	private ServerSocket serverSocket;
	private boolean alive;
	
	public PortListener(Node n)
	{
		local = n;
		alive = true;
		InetSocketAddress localAddress = local.getAddress();
		int port = localAddress.getPort();
		
		try //opens new socket
		{
			serverSocket = new ServerSocket(port);
		}
		
		catch(IOException e)
		{
			throw new RuntimeException("Error opening port: "+port+". Exiting Program...", e);
		}
	}
	
	@Override
	public void run()
	{
		while(alive)
		{
			Socket talkSocket = null;
			try
			{
				talkSocket = serverSocket.accept();
			}
			
			catch(IOException e)
			{
				throw new RuntimeException("Cannot accept connection:", e);
			}
			
			new Thread(new TalkToSocket(talkSocket, local)).start(); //create new talker
		}
	}
	
	public void toDie()
	{
		alive = false;
	}
	
}//END PORTLISTENER
