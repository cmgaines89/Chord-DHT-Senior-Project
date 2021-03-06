/*
C. Gaines
Chord Project
*/

import java.util.*;
import java.io.*;
import java.net.*;

/*Thread that processes requests accepted by listener node and writes the responses to the socket*/

public class TalkToSocket implements Runnable
{

	Socket talkSocket;
	private Node local;

	public TalkToSocket(Socket _talkSocket, Node _local)
	{
		talkSocket = _talkSocket;
		local = _local;
	}

	public void run()
	{
		InputStream input = null;
		OutputStream output = null;
		
		try
		{
			input = talkSocket.getInputStream();
			String request = Hasher.inputStreamToString(input);
			String response = processRequest(request);
			
			if(response != null)
			{
				output = talkSocket.getOutputStream();
				output.write(response.getBytes());
			}
			
			input.close();
		}
		
		catch (IOException e)
		{
			throw new RuntimeException("Cannot talk.\nServer port: "+local.getAddress().getPort()+"; Talker port: "+talkSocket.getPort(), e);
		}
	}

	private String processRequest(String request)
	{
		InetSocketAddress result = null;
		String ret = null;
		
		if(request  == null)
		{
			return null;
		}
		
		if(request.startsWith("CLOSEST"))
		{
			long id = Long.parseLong(request.split("_")[1]);
			result = local.closest_preceding_finger(id);
			String ip = result.getAddress().toString();
			int port = result.getPort();
			ret = "MYCLOSEST_"+ip+":"+port;
		}
		
		else if(request.startsWith("YOURSUCC"))
		{
			result =local.getSuccessor();
			
			if(result != null)
			{
				String ip = result.getAddress().toString();
				int port = result.getPort();
				ret = "MYSUCC_"+ip+":"+port;
			}
			
			else
			{
				ret = "NOTHING";
			}
		}
		
		else if(request.startsWith("YOURPRE"))
		{
			result =local.getPredecessor();
			
			if(result != null)
			{
				String ip = result.getAddress().toString();
				int port = result.getPort();
				ret = "MYPRE_"+ip+":"+port;
			}
			
			else
			{
				ret = "NOTHING";
			}
		}
		
		else if(request.startsWith("FINDSUCC"))
		{
			long id = Long.parseLong(request.split("_")[1]);
			result = local.find_successor(id);
			String ip = result.getAddress().toString();
			int port = result.getPort();
			ret = "FOUNDSUCC_"+ip+":"+port;
		}
		
		else if(request.startsWith("IAMPRE"))
		{
			InetSocketAddress new_pre = Hasher.createSocketAddress(request.split("_")[1]);
			local.notified(new_pre);
			ret = "NOTIFIED";
		}
		
		else if (request.startsWith("KEEP"))
		{
			ret = "ALIVE";
		}
		
		return ret;
	}
	
}//END TALKTOSOCKET
