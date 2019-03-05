package Assignment_1;

import java.io.DataOutputStream;
import java.net.ServerSocket;
import java.net.Socket;

public class Assignment_1 {
	public static void main(String args[]) throws Exception {
		
		// set up a server socket to listen for incoming connections
		ServerSocket myServerSocket = new ServerSocket(1235);
		System.out.println("Started server on port " + myServerSocket.getLocalPort());
		
		while(true){
			 
			//wait until client connects, then assign new client socket
			Socket connectedClientSocket = myServerSocket.accept();
			System.out.println("\nClient connected on port " + connectedClientSocket.getLocalPort());
			
			// set up an output stream to the client
			DataOutputStream outputStream = new DataOutputStream(connectedClientSocket.getOutputStream());
			
			// construct the response
			String connectionResponse = "Hello " + connectedClientSocket.getInetAddress()
					+ " " + connectedClientSocket.getPort();
			
			// send the response
			outputStream.writeBytes(connectionResponse);
			
			System.out.println("\nResponse:");
			System.out.println(connectionResponse);
			
			// disconnected the client and loop back to waiting for a new client
			connectedClientSocket.close();
			System.out.println("\nClient disconnected by server");
		}
	}
}