/*
Corinne Gaines
COSC 495 - Fall 2020
Chord Project
*/

import java.util.*;
import java.io.*;
import java.net.*;

/*Continuoiusly checks predecessor thread and periodically asks for the predecessor nodes keep-alive signal
and will delete the predecessor if its dead*/

public class CheckPredecessor extends Thread
{
	private Node local;
	private boolean alive;
	
	public CheckPredecessor(Node _local) {
		local = _local;
		alive = true;
	}
	
	@Override
	public void run() {
		while (alive) {
			InetSocketAddress predecessor = local.getPredecessor();
			if (predecessor != null) {
				String response = Hasher.sendRequest(predecessor, "KEEP");
				if (response == null || !response.equals("ALIVE")) {
					local.clearPredecessor();	
				}

			}
			try {
				Thread.sleep(500);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
	}
	
	public void toDie() {
		alive = false;
	}
	
} //END CHECKPREDECESSOR
	
