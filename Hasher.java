/*
Corinne Gaines
COSC 495 - Fall 2020
Chord Project
*/

import java.util.*;
import java.io.*;
import java.net.*;
import java.security.*;

public class Hasher
{
	private static HashMap<Integer, Long> powOfTwo = null;
	
	
	public Hasher()
	{ //Hasher Constructor
	
		powOfTwo = new HashMap<Integer, Long>(); //initializes a power of two hash table
		long base = 1;
		
		for(int i = 0; i <= 32; i++)
		{
			powOfTwo.put(i, base);
			base *= 2;
		}
	}
	
	
	/*HASHING FUNCTIONS*/
			
	private static long hashHashCode (int i)
	{ //Computes a 32-bit integers identifier

		byte[] hashbytes = new byte[4]; //32-bit regular hash code to byte [4]
		hashbytes[0] = (byte) (i >> 24);
		hashbytes[1] = (byte) (i >> 16);
		hashbytes[2] = (byte) (i >> 8);
		hashbytes[3] = (byte) (i); // >> 0

		MessageDigest md =  null; //Tries to create SHA-1 digest
		try
		{
			md = MessageDigest.getInstance("SHA-1");
		} 
		
		catch (NoSuchAlgorithmException e)
		{
			e.printStackTrace();
		}
	
		if (md != null) //if SHA-1 digest is successfully created convert byte [4]
		{
			md.reset();
			md.update(hashbytes);
			byte[] result = md.digest();
			byte[] compressed = new byte[4]; //compress the result for byte [4]
			for (int j = 0; j < 4; j++)
			{
				byte temp = result[j];
				for (int k = 1; k < 5; k++)
				{
					temp = (byte) (temp ^ result[j+k]);
				}
				compressed[j] = temp;
			}

			long ret = (compressed[0] & 0xFF) << 24 | (compressed[1] & 0xFF) << 16 | (compressed[2] & 0xFF) << 8 | (compressed[3] & 0xFF);
			ret = ret&(long)0xFFFFFFFFl; //compressed result in type long
			return ret; //return 32-bit identifier
		}
		return 0;
	}
	
	
	public static long hashSocketAddress(InetSocketAddress inet)
	{ //Get a sockets 32-bit address identifier
	
		int i = inet.hashCode();
		return hashHashCode(i);
	}
	
	
	public static long hashString(String str)
	{ //Get a strings 32-bit identifier
	
		int i = str.hashCode();
		return hashHashCode(i);
	}
	
	
	/*COMPUTE FUNCTIONS*/
	
	public static long ithStart (long nodeid, int i)
	{ //Universal construct to return a nodes start in the finger table - finger[i]
	
		return (nodeid + powOfTwo.get(i-1)) % powOfTwo.get(32);
	}
	
	public static long getPowofTwo(int k)
	{ //Get pow of 2
	
		return powOfTwo.get(k);
	}
	
	public static long computeRelativeId(long universal, long local)
	{ //Compute universal ID's value in realtion to its local ID [local node is regarded as 0]
	
		long ret = universal - local;
		
		if(ret < 0)
		{
			ret += powOfTwo.get(32);
		}
		
		return ret;
	}
	
	public static String longTo8DigitHex(long l)
	{ //Generates a numbers 8 digit hex string in type long
	
		String hex = Long.toHexString(l);
		int lack = 8-hex.length();
		
		StringBuilder sb = new StringBuilder();
		for (int i = lack; i > 0; i--)
		{
			sb.append("0");
		}
		
		sb.append(hex);
		return sb.toString();
	}
	
	public static String hexIdAndPosition(InetSocketAddress addr)
	{ //Computes a socket address in SHA-1 hash in hex and its approximate porision in type string
	
		long hash = hashSocketAddress(addr);
		return (longTo8DigitHex(hash)+" ("+hash*100/Hasher.getPowofTwo(32)+"%)");
	}
	
	
	/*REQUST FUNCTIONS*/
	
	public static InetSocketAddress createSocketAddress (String str)
	{ //Creates socket address using IP address and port number
		
		if(str == null)
			return null;
			
		String[] split = str.split(":"); //splits string
		if(split.length >= 2)
		{
			String ip = split[0]; //gets and preprocesses IP address string
			if(ip.startsWith("/"))
			{
				ip = ip.substring(1);
			}
				
			InetAddress inet = null; //parses IP address and returns null on fail
			try
			{
				inet = InetAddress.getByName(ip);
			}
		
			catch(UnknownHostException e)
			{
				System.out.println("IP Address cannot be created: "+ip);
				return null;
			}
			
			String port = split[1]; //parses port number
			int i_port = Integer.parseInt(port);
			return new InetSocketAddress(inet, i_port); //combines IP address and port number into socket address
		}
		
		else //cannot split string
			return null;
	}
	
	
	public static InetSocketAddress requestAddress (InetSocketAddress server, String req)
	{ //Generates requested address by sending a request to the server

		if(server == null || req == null) //invalid input, return null
		{
			return null;
		}

		String response = sendRequest(server, req); //sends the request to the server

		if(response == null) //if no response, return null
		{
			return null;
		}

		else if(response.startsWith("NOTHING")) //if the server cannot find anything, the server returns itself	
			return server;

		else //server found something, use response to create, if it fails return null
		{
			InetSocketAddress ret = Hasher.createSocketAddress(response.split("_")[1]);
			return ret;
		}
	}
	
	
	public static String sendRequest(InetSocketAddress server, String req)
	{ //Sends request to the server and reads its response
		
		if(server == null || req == null) //invalid input handler
			return null;

		Socket talkSocket = null;

		try //try to open a talkSocket and output this request to this socket
		{
			talkSocket = new Socket(server.getAddress(),server.getPort());
			PrintStream output = new PrintStream(talkSocket.getOutputStream());
			output.println(req);
		}
		
		catch(IOException e)
		{
			//System.out.println("\nCannot send request to "+server.toString()+"\nRequest is: "+req+"\n");
			return null;
		}

		try //sleep, while waiting for server response
		{
			Thread.sleep(60);
		}
		
		catch(InterruptedException e)
		{
			e.printStackTrace();
		}

		InputStream input = null; //get users input
		try
		{
			input = talkSocket.getInputStream();
		} 
		
		catch(IOException e)
		{
			System.out.println("Cannot get input stream from "+server.toString()+"\nRequest is: "+req+"\n");
		}
		String response = Hasher.inputStreamToString(input);

		try //close socket
		{
			talkSocket.close();
		}
		 
		catch(IOException e)
		{
			throw new RuntimeException("Cannot close socket", e);
		}
		
		return response;
	}
	
	
	/*PRINTABLES/INPUTS/EXCEPTIONS*/
	
	public static String inputStreamToString (InputStream in)
	{ //Reads a line from the input stream

		if (in == null)
		{
			return null;
		}

		BufferedReader reader = new BufferedReader(new InputStreamReader(in));
		String line = null;
		try //read line from the input stream
		{
			line = reader.readLine();
		} 
		
		catch (IOException e) //catch if nothing can be read from the input stream
		{
			System.out.println("Cannot read line from input stream.");
			return null;
		}

		return line;
	}
		
} //END HASHER
