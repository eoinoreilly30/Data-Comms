package Assignment_3;

import java.io.*; 
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

public class Client {
	
	public static void main(String argv[]) throws Exception 
	{	
		//read in local cookies database
		Path pathToCookies;
		List<String> cookies = null;
		try {
			pathToCookies = Paths.get("src/clientCookies.txt");
			cookies = new ArrayList<>(Files.readAllLines(pathToCookies, StandardCharsets.UTF_8));
		}
		catch(NoSuchFileException e) {
			System.out.println("ERROR opening database.\nPlace database in 'src' folder and name it clientCookies.txt");
			return;
		}
		
		// parse the cookie from the database
		String cookie = null;
		try {
			cookie = cookies.get(0);
			
			// if cookie file is blank
			if(cookie.equals("\n")) {
				System.out.println("No cookies on device");
				cookie = "userid=-1";
			}
		}
		// also happens when cookie file is blank
		catch (IndexOutOfBoundsException e) {
			System.out.println("No cookies on device");
			cookie = "userid=-1";
		}
		
		System.out.println("\nConnecting to server...");
	    Socket clientSocket = new Socket("localhost", 1245);
	    System.out.println("Connected to server\n");
	    
	    // setup i/o to server
	    DataOutputStream outToServer = new DataOutputStream(clientSocket.getOutputStream());
	    BufferedReader inFromServer = new BufferedReader(new InputStreamReader(clientSocket.getInputStream())); 
	    BufferedReader inFromUser = new BufferedReader(new InputStreamReader(System.in));
	    
	    // send cookie to server
		outToServer.writeBytes(cookie + "\n");
	    
	    // begin exchanges with server
		while(true) {
			
			if(inFromServer.ready()) {
				while(inFromServer.ready()) {
					String received = inFromServer.readLine();
					
					// special case of where client needs to update cookie
					if(received.equals("INCOMING_COOKIE")) {
						cookies.add(inFromServer.readLine());
						Files.write(pathToCookies, cookies, StandardCharsets.UTF_8);
					}
					
					// special case of disconnect signal from server
					else if (received.equals("DISCONNECT")) {
						System.out.println("You have been disconnected");
						clientSocket.close();
						return;
					}
					else {
						System.out.println(received);
					}
				}
			}
			
			else if(inFromUser.ready()) {
				String input = inFromUser.readLine();
				outToServer.writeBytes(input + "\n");
			}
		}
	}
}
