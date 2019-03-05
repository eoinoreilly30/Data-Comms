package Assignment_2;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;


public class Assignment_2_Exercise_1 {
	public static void main(String args[]) throws Exception {
		
		ServerSocket myServerSocket = new ServerSocket(1270);
		System.out.println("Started server on port " + myServerSocket.getLocalPort());
		
		while(true){			
			Socket connectedClientSocket = myServerSocket.accept();
			System.out.println("\nClient connected");
			
			//set up output stream
			DataOutputStream outToClient = new DataOutputStream(connectedClientSocket.getOutputStream());
			
			outToClient.writeBytes("\r\nYou're in\r\n");
			
			while(!connectedClientSocket.isClosed()){
				
				outToClient.writeBytes("\r\nEnter item\r\n");
				
				//set up input stream
				BufferedReader inFromClient = new BufferedReader(
						new InputStreamReader(connectedClientSocket.getInputStream())); 
				
				//read product request and confirm with client
				String requestedProduct = inFromClient.readLine();
				System.out.println("Server Received: " + requestedProduct);
				outToClient.writeBytes("\r\nServer Received: " + requestedProduct + "\r\n");
				
				//read in database
				Path path = null;
				List<String> fileContent = null;
				try {
					path = Paths.get("src/database.txt");
					fileContent = new ArrayList<>(Files.readAllLines(path, StandardCharsets.UTF_8));
				}
				catch(NoSuchFileException e) {
					System.out.println("ERROR opening database.\nPlace database in 'src' folder and name it database.txt");
					outToClient.writeBytes("\r\nERROR opening database. Call tech support\r\n\r\n");
					System.out.println("Client disconnected");
					connectedClientSocket.close();
					break;
				}		
			    	
				//default response
				String response = "\r\nNo Item Found\r\n";
				
				//check if item is in database
				for(String line : fileContent){
			    	String[] itemInfo = line.split(":");
			    	
			    	if(itemInfo[0].equals(requestedProduct)){
			    		System.out.println("Found product");
			    		response = "\r\nItem: " + itemInfo[0] + "\r\nQuantity: " + itemInfo[1] 
			    				+ "\r\nPrice: " + itemInfo[2] + "\r\n";
			    		break;
			    	}
			    }

				outToClient.writeBytes(response);
				System.out.println(response);
				
				//ask if client wants to search again
				outToClient.writeBytes("\r\nSearch again? (y/n)\r\n");
				String yn = inFromClient.readLine();
				if(yn.equals("n")){
					outToClient.writeBytes("\r\nGoodbye\r\n\r\n");
					connectedClientSocket.close();
					System.out.println("Client disconnected by server");
				}
			}
		}
	}
}
