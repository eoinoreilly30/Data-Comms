package Assignment_3;

import java.io.*;
import java.util.Random;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

public class Server {
	
	// products database variables
	private List<String> fileContent;
	private List<String> items;
	private List<Integer> quantities;
	private List<Double> prices;

	// user info database variables
	private Random rand;
	private List<Integer> userids;
	private List<String> usernames;
	private List<String> usertypes;
	private List<String> userInfoDatabase;
	private ArrayList<ArrayList<String>> purchases;
	
	private Path pathToDatabase;
	private Path pathToUserDatabase;
	
	private ServerSocket myServerSocket;
	private Socket clientSocket;
	
	private DataOutputStream outToClient;
	private BufferedReader inFromClient;
	
	private int userLine;
	
	public Server(int portNo) throws IOException {
		
		myServerSocket = new ServerSocket(portNo);
		System.out.println("Started server on " + myServerSocket.getLocalPort());
	}
	
	// returns the line number that the item is found at, otherwise -1
	private int findItemLineNumber(String item){
		for (int i = 0; i < fileContent.size(); i++) {
			String[] line = fileContent.get(i).split(":");
		    if (line[0].equals(item)){
		        return i;
		    }
		}
		return -1;
	}
	
	// returns the line number that the cookie is found at, otherwise -1
	private int findCookieLineNumber(String cookie){
		for (int i = 0; i < userInfoDatabase.size(); i++) {
			String[] line = userInfoDatabase.get(i).split(":");
		    if (line[0].equals(cookie)){
		        return i;
		    }
		}
		return -1;
	}
	
	// returns the line number that the username is found at, otherwise -1
	private int findUserLineNumber(String user){
		for (int i = 0; i < userInfoDatabase.size(); i++) {
			String[] line = userInfoDatabase.get(i).split(":");
		    if (line[1].split("=")[1].equals(user)){
		        return i;
		    }
		}
		return -1;
	}
	
	private boolean readInDatabases() throws IOException {
		
		// products data
		fileContent = new ArrayList<String>();
		items = new ArrayList<String>();
		quantities = new ArrayList<Integer>();
		prices = new ArrayList<Double>();
		
		// user info data
		userInfoDatabase = new ArrayList<String>();
		userids = new ArrayList<Integer>();
		usernames = new ArrayList<String>();
		usertypes = new ArrayList<String>();
		purchases = new ArrayList<ArrayList<String>>();
		rand = new Random();
		
		// read in products database
		try {
			pathToDatabase = Paths.get("src/productsDatabase.txt");
			fileContent = Files.readAllLines(pathToDatabase, StandardCharsets.UTF_8);
		}
		catch(NoSuchFileException e) {
			System.out.println("ERROR opening database.\nPlace database in 'src' folder and name it productsDatabase.txt");
			return false;
		}
		
		for(String line : fileContent) {
			String[] lineArray = line.split(":");
			items.add(lineArray[0]);
			quantities.add(Integer.parseInt(lineArray[1]));
			prices.add(Double.parseDouble(lineArray[2]));
		}
		
		// read in user information database
		try {
			pathToUserDatabase = Paths.get("src/userInfoDatabase.txt");
			userInfoDatabase = Files.readAllLines(pathToUserDatabase, StandardCharsets.UTF_8);
		}
		catch(NoSuchFileException e) {
			System.out.println("ERROR opening database.\nPlace database in 'src' folder and name it userInfoDatabase.txt");
			return false;
		}
		
		try {
			int i=0;
			for(String line : userInfoDatabase) {
				String[] lineArray = line.split(":");
				userids.add(Integer.parseInt(lineArray[0].split("=")[1]));
				usernames.add(lineArray[1].split("=")[1]);
				usertypes.add(lineArray[2].split("=")[1]);
				purchases.add(new ArrayList<String>());
				for(int j=3; j<lineArray.length; j++){
					purchases.get(i).add(lineArray[j]);
				}
				i++;
			}
		}
		catch (IndexOutOfBoundsException e) {
			System.out.println("User Info database empty");
			// add dummy data to avoid index out of bounds exceptions
			userids.add(1000);
			usernames.add("xxxxxx");
			usertypes.add("xxxxxx");
			purchases.add(new ArrayList<String>());
		}
		
		return true;
	}
	
	private void waitForConnection() throws IOException {
		System.out.println("Waiting for new connection");
		
		// wait for clients
		clientSocket = myServerSocket.accept();
		System.out.println("Client connected");
		
		// setup i/o to client
		outToClient = new DataOutputStream(clientSocket.getOutputStream());
		inFromClient = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
	}
	
	private boolean loginClient() throws IOException {
		outToClient.writeBytes("Enter username\n");
		String username = inFromClient.readLine();
		
		int usernameLineNumber = findUserLineNumber(username);
		
		if(usernameLineNumber == -1) {
			outToClient.writeBytes("No user found\n");
			return false;
		}
		
		else {
			userLine = usernameLineNumber;
			
			// send the cookie to the client
			outToClient.writeBytes("INCOMING_COOKIE\n" + "userid=" + userids.get(userLine) + "\n");
			outToClient.writeBytes("Welcome back " + username + "!\n");
			return true;
		}
	}
	
	private boolean signUpNewClient() throws IOException {
		
		outToClient.writeBytes("\nEnter username:\n");
		String username = inFromClient.readLine();
		
		if(findUserLineNumber(username) == -1) {
			usernames.add(username);
		}
		else {
			outToClient.writeBytes("Username already taken\n");
			return false;
		}
		
		outToClient.writeBytes("Enter user type [customer/vendor]\n");
		String type = inFromClient.readLine().toLowerCase();
		
		if(type.equals("customer") || type.equals("vendor")) {
			usertypes.add(type);
		}
		else {
			outToClient.writeBytes("ERROR: Please spell correctly\n");
			return false;
		}
			
		// update cookies database
		int userid = rand.nextInt(1000);
		String newLine = "userid=" + userid + ":username=" + username + ":usertype=" + type;
		userInfoDatabase.add(newLine);
		userLine = userInfoDatabase.size() - 1;
		Files.write(pathToUserDatabase, userInfoDatabase, StandardCharsets.UTF_8);
		
		// send the cookie to the client
		outToClient.writeBytes("INCOMING_COOKIE\n" + "userid=" + userid + "\n");
		
		return true;
	}
	
	private boolean handleCustomer() throws IOException {
		
		// send menu to client
		outToClient.writeBytes("\nMenu:\nSearch [Display info about a product]\n"
				+ "Buy [Buy a product]\nExit [Exit the menu]\n");
		
		switch(inFromClient.readLine().toLowerCase()) {
		
		case "search":
			
			outToClient.writeBytes("\nEnter product name:\n");
			String requestedItemToSearch = inFromClient.readLine();
			
			int itemLineNumber = findItemLineNumber(requestedItemToSearch);
			
			// if the requested item is found, send info to client
			if(itemLineNumber >= 0){
				outToClient.writeBytes("\nItem: " + items.get(itemLineNumber) + 
						"\nQuantity: " + quantities.get(itemLineNumber) + "\nPrice: " + prices.get(itemLineNumber) + "\n");
				return true;
			}
			
			// if not found, return false
			else {
				outToClient.writeBytes("\nNo item found\n");
				return false;
			}
		
		case "buy":
			
			outToClient.writeBytes("\nEnter product name:\n");
			String requestedItemToBuy = inFromClient.readLine();
			
			itemLineNumber = findItemLineNumber(requestedItemToBuy);
			
			// if the requested item is not found, return false
			if(itemLineNumber < 0){
				outToClient.writeBytes("\nNo item found\n");
				return false;
			}
			
			int currentQuantity = quantities.get(itemLineNumber);
			
			// if item not sold out, decrease quantity by 1 and update database
			if(currentQuantity > 0){
				String newBuyLine = items.get(itemLineNumber) + ":" + (currentQuantity-1) + ":" + prices.get(itemLineNumber);
				fileContent.set(itemLineNumber, newBuyLine);
				
				// record that this user has purchased the item
				String newDatabaseLine = userInfoDatabase.get(userLine) + ":" + requestedItemToBuy;
				userInfoDatabase.set(userLine, newDatabaseLine);
				outToClient.writeBytes("\nBought successfully\n");
				outToClient.writeBytes("\nNew quantity: " + (currentQuantity-1) + "\n");
				outToClient.writeBytes("\nYour item will be delivered asap!\n");
				
				return true;
			}
			else {
				outToClient.writeBytes("\nItem sold out\n");
				return false;
			}
		
		case "exit":
			return true;
			
		default:
			outToClient.writeBytes("\nERROR: Enter one of the options above\n");
			return false;
		}
	}

	private boolean handleVendor() throws IOException {
		
		// send menu to client
		outToClient.writeBytes("\nMenu:\nSearch [Display info about a product]\n"
				+ "Restock [Change a product's quantity]\nReprice [Change a product's price]\n"
				+ "History [Check a customers purchases]\nExit [Exit the menu]\n");
		
		switch(inFromClient.readLine().toLowerCase()) {
		
			case "search":
			
				outToClient.writeBytes("\nEnter product name:\n");
				String requestedItemToSearch = inFromClient.readLine();
				
				int itemLineNumber = findItemLineNumber(requestedItemToSearch);
				
				// if the requested item is found, send info to client
				if(itemLineNumber >= 0){
					outToClient.writeBytes("\nItem: " + items.get(itemLineNumber) + 
							"\nQuantity: " + quantities.get(itemLineNumber) + "\nPrice: " + prices.get(itemLineNumber) + "\n");
					return true;
				}
				
				// if not found, return false
				else {
					outToClient.writeBytes("\nNo item found\n");
					return false;
				}
				
			case "restock":
				
				outToClient.writeBytes("\nWhat item?\n");
				String requestedItemToRestock = inFromClient.readLine();
				
				// find the item
				itemLineNumber = findItemLineNumber(requestedItemToRestock);
				if(itemLineNumber < 0){
					outToClient.writeBytes("\nNo item found\n");
					return false;
				}
				
				// send current quantity
				int currentQuantity = quantities.get(itemLineNumber);
				outToClient.writeBytes("\nCurrent amount: " + currentQuantity + "\n");
				
				// get new amount
				outToClient.writeBytes("\nEnter new amount\n");
				String newRequestedQuantity = inFromClient.readLine();
				
				// check for correct formatting
				int newQuantity = 0;
				try {
					newQuantity = Integer.parseInt(newRequestedQuantity);
				}
				catch (NumberFormatException e) {
					outToClient.writeBytes("\nError: Please enter an integer\n");
					return false;
				}
				
				if(newQuantity >= 0) {
					String newQuantityLine = items.get(itemLineNumber) + ":" + newQuantity + ":" + prices.get(itemLineNumber);
					fileContent.set(itemLineNumber, newQuantityLine);
					outToClient.writeBytes("\nRestocked successfully\n");
					outToClient.writeBytes("\nNew amount: " + newQuantity + "\n");
					return true;
				}
				else {
					outToClient.writeBytes("\nERROR: Please enter a positive quantity\n");
					return false;
				}
				
			case "reprice":
				
				outToClient.writeBytes("\nWhat item?\n");
				String requestedItemToReprice = inFromClient.readLine();
				
				// find item
				itemLineNumber = findItemLineNumber(requestedItemToReprice);
				if(itemLineNumber < 0){
					outToClient.writeBytes("\nNo item found\n");
					return false;
				}
				
				// send current price
				Double currentPrice = prices.get(itemLineNumber);
				outToClient.writeBytes("\nCurrent price: " + currentPrice +  "\n");
				
				// get new price
				outToClient.writeBytes("\nEnter new price\n");
				String newRequestedPrice = inFromClient.readLine();
				
				// check for correct formatting
				Double newPrice = 0.0;
				try {
					newPrice = Double.parseDouble(newRequestedPrice);
				}
				catch (NumberFormatException e) {
					outToClient.writeBytes("\nERROR: Please enter a number\n");
					return false;
				}
				
				if(newPrice >= 0){
					String newPriceLine = items.get(itemLineNumber) + ":" + quantities.get(itemLineNumber) + ":" + newPrice;
					fileContent.set(itemLineNumber, newPriceLine);
					outToClient.writeBytes("\nPrice updated successfully\n");
					outToClient.writeBytes("\nNew price: " + newPrice + "\n");
					return true;
				}
				else {
					outToClient.writeBytes("\nERROR: Please enter a positive price\n");
					return false;
				}
				
			case "history":
				
				outToClient.writeBytes("\nWhich customer?\n");
				String requestedCustomer = inFromClient.readLine();
				
				// find customer
				int userLineNumber = findUserLineNumber(requestedCustomer);
				if(userLineNumber < 0){
					outToClient.writeBytes("\nNo user found\n");
					return false;
				}
				
				// print customers purchases
				outToClient.writeBytes("\n" + requestedCustomer + " has purchased:\n");
				for(String purchase : purchases.get(userLineNumber)){
					outToClient.writeBytes(purchase + "\n");
				}
				
				return true;
					
			case "exit":
				return true;
				
			default:
				outToClient.writeBytes("\nERROR: Input a menu option from above\n");
				return false;
		}
	}
	
	private boolean tryAgain() throws IOException {
		outToClient.writeBytes("\nWould you like to try again? [y/n]\n");
		
		if(inFromClient.readLine().equals("y")) {
			return true;
		}
		else {
			outToClient.writeBytes("Disconnecting...\n");
			
			// send client the disconnect signal
			outToClient.writeBytes("DISCONNECT\n");
			return false;
		}
	}
	
	private void identifyClient() throws IOException {
		
		String clientCookie = inFromClient.readLine();
		System.out.println("Client cookie: " + clientCookie);
		userLine = findCookieLineNumber(clientCookie);
		
		// client is new, ask for login or sign them up
		if(userLine < 0) {
			System.out.println("Client cookie not recognised");			
			
			outToClient.writeBytes("Welcome!\nThe Smart-Cookie-System has identified you as a new client\n");
			
			while(true) {
				outToClient.writeBytes("Please sign up (s) or login (l)\n");
				
				String input = inFromClient.readLine();
				if(input.equals("s")) {
					while(signUpNewClient() == false);
					break;
				}
				
				else if(input.equals("l")) {
					while(loginClient() == false);
					break;
				}
				else {
					outToClient.writeBytes("Enter s or l\n");
				}
			}
		}
			
		// client is a returning client
		else {
			String username = usernames.get(userLine);
			outToClient.writeBytes("Welcome back " + username + "!\n");
		}
	}
	
	private void clientServerExchange() throws IOException {
		
		// begin exchange with client
		while(true){	
			
			// read in databases
			if(readInDatabases()) {
				System.out.println("Successfully read in databases");
			}
			
			// customer
			if(usertypes.get(userLine).equals("customer")) {
				while(handleCustomer() == false);
			}
			
			// vendor
			else {
				while(handleVendor() == false);
			}
			
			// write out databases
			Files.write(pathToDatabase, fileContent, StandardCharsets.UTF_8);
			Files.write(pathToUserDatabase, userInfoDatabase, StandardCharsets.UTF_8);
			System.out.println("Successfully saved databases");
			
			// ask client if they would like to try again
			if(tryAgain() == false) {
				System.out.println("Client disconnected");
				return;
			}
		}
	}
	
	public static void main(String args[]) throws IOException {
		
		// set up server
		Server myServer = new Server(1245);
		
		// read in databases
		if(myServer.readInDatabases())
			System.out.println("Successfully read in databases");
		else
			return;
		
		while(true){		
			myServer.waitForConnection();
			myServer.identifyClient();
			myServer.clientServerExchange();
		}
	}
}
