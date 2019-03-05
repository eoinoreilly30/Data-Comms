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
import java.util.Arrays;
import java.util.List;

public class Assignment_2_Exercise_2 {
	
	// returns the line number that the item is found at, otherwise -1
	private static int findItemLineNumber(List<String> file, String item){
		for (int i = 0; i < file.size(); i++) {
			String[] line = file.get(i).split(":");
		    if (line[0].equals(item)){
		        return i;
		    }
		}
		return -1;
	}
	
	public static void main(String args[]) throws Exception {
			
		ServerSocket myServerSocket = new ServerSocket(1245);
		System.out.println("Started server on " + myServerSocket.getLocalPort());
		
		while(true){
			
			// establish a server socket to wait for clients
			Socket connectedClientSocket = myServerSocket.accept();
			System.out.println("Client connected");
			
			// output connection to client
			DataOutputStream outToClient = new DataOutputStream(connectedClientSocket.getOutputStream());
			
			outToClient.writeBytes("\r\nYou're in\r\n");
			
			while(connectedClientSocket.isConnected()){
				
				// client menu prompt
				outToClient.writeBytes("\r\nBUY [Buy an item]\r\n");
				outToClient.writeBytes("RESTOCK [Restock an item]\r\n");
				outToClient.writeBytes("ADD [Add an item]\r\n");
				outToClient.writeBytes("PRICE [Change an item's price]\r\n");
				
				// input connection from client
				BufferedReader inFromClient = new BufferedReader(
						new InputStreamReader(connectedClientSocket.getInputStream())); 
				
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
				
				String receivedFromClient = null;
				String[] lineArray = null;
				int itemLineNumber;
				
				// menu implementation
				String input = inFromClient.readLine().toUpperCase();	
				switch(input){
					
					case "BUY": 
								int currentQuantity = 0;
								String newBuyLine = null;
								
								outToClient.writeBytes("\r\nWhat item?\r\n");
								receivedFromClient = inFromClient.readLine();
								
								// if the requested item is found, split it into its parts
								itemLineNumber = findItemLineNumber(fileContent, receivedFromClient);
								if(itemLineNumber >= 0){
									lineArray = fileContent.get(itemLineNumber).split(":");
								}
								// if not found, break
								else {
									outToClient.writeBytes("\r\nNo item found\r\n");
									break;
								}
								
								// parse the quantity from the database
								try {
									currentQuantity = Integer.parseInt(lineArray[1]);
								}
								catch(NumberFormatException e){
									outToClient.writeBytes("\r\nERROR reading database\r\n");
									System.out.println("Error reading database");
									break;
								}
								
								// if item not sold out, decrease quantity by 1 and update database
								if(currentQuantity > 0){
									lineArray[1] = Integer.toString(currentQuantity - 1);
									newBuyLine = lineArray[0] + ":" + lineArray[1] + ":" + lineArray[2];
									fileContent.set(itemLineNumber, newBuyLine);
									outToClient.writeBytes("\r\nBought successfully\r\n");
									outToClient.writeBytes("\r\nNew quantity: " + lineArray[1] + "\r\n");
									outToClient.writeBytes("\r\nYour item will be delivered asap!\r\n");
								}
								else {
									outToClient.writeBytes("\r\nItem sold out\r\n");
								}
								
								break;
								
					case "RESTOCK": 
								String newQuantityLine = null;
								
								outToClient.writeBytes("\r\nWhat item?\r\n");
								receivedFromClient = inFromClient.readLine();
								
								itemLineNumber = findItemLineNumber(fileContent, receivedFromClient);
								if(itemLineNumber >= 0){
									lineArray = fileContent.get(itemLineNumber).split(":");
								}
								else {
									outToClient.writeBytes("\r\nNo item found\r\n");
									break;
								}
								
								currentQuantity = Integer.parseInt(lineArray[1]);
								outToClient.writeBytes("\r\nCurrent amount: " + currentQuantity + "\r\n");
								outToClient.writeBytes("\r\nEnter new amount\r\n");
								receivedFromClient = inFromClient.readLine();
								
								int newQuantity = 0;
								try {
									newQuantity = Integer.parseInt(receivedFromClient);
								}
								catch (NumberFormatException e) {
									outToClient.writeBytes("\r\nError: Please enter a number\r\n");
									break;
								}
								
								if(newQuantity >= 0){
									lineArray[1] = Integer.toString(newQuantity);
									newQuantityLine = lineArray[0] + ":" + lineArray[1] + ":" + lineArray[2];
									fileContent.set(itemLineNumber, newQuantityLine);
									outToClient.writeBytes("\r\nRestocked successfully\r\n");
									outToClient.writeBytes("\r\nNew amount: " + lineArray[1] + "\r\n");
								}
								else {
									outToClient.writeBytes("\r\nERROR: Please enter a positive quantity\r\n");
								}
								
								break;
								
					case "ADD": 
								String newLine = null;
								
								// Item
								outToClient.writeBytes("\r\nWhat item?\r\n");
								newLine = inFromClient.readLine() + ":";
								
								// Quantity
								outToClient.writeBytes("\r\nWhat quantity?\r\n");
								int amount = 0;
								try {
									amount = Integer.parseInt(inFromClient.readLine());
								}
								catch (NumberFormatException e) {
									outToClient.writeBytes("\r\nERROR: Please enter an integer\r\n");
									break;
								}
								
								if(amount > 0) {
									newLine += amount + ":";
								}
								else {
									outToClient.writeBytes("\r\nERROR: Please enter a quantity greater than 0\r\n");
									break;
								}
								
								// Price
								outToClient.writeBytes("\r\nWhat price?\r\n");
								double price = 0;
								try {
									price = Double.parseDouble(inFromClient.readLine());
								}
								catch (NumberFormatException e) {
									outToClient.writeBytes("\r\nERROR: Please enter a number\r\n");
									break;
								}
								
								if(price >= 0) {
									newLine += price;
								}
								else {
									outToClient.writeBytes("\r\nERROR: Please enter a price greater than 0\r\n");
									break;
								}
								
								fileContent.add(newLine);
								outToClient.writeBytes("\r\nItem added successfully\r\n");
								
								break;
								
					case "PRICE": 
								String newPriceLine = null;
								Double newPrice = 0.0;
								
								outToClient.writeBytes("\r\nWhat item?\r\n");
								receivedFromClient = inFromClient.readLine();
								
								itemLineNumber = findItemLineNumber(fileContent, receivedFromClient);
								if(itemLineNumber >= 0){
									lineArray = fileContent.get(itemLineNumber).split(":");
								}
								else {
									outToClient.writeBytes("\r\nNo item found\r\n");
									break;
								}
								
								outToClient.writeBytes("\r\nCurrent price: " + lineArray[2] +  "\r\n");
								outToClient.writeBytes("\r\nEnter new price\r\n");
								receivedFromClient = inFromClient.readLine();
								
								try {
									newPrice = Double.parseDouble(receivedFromClient);
								}
								catch (NumberFormatException e) {
									outToClient.writeBytes("\r\nERROR: Please enter a number\r\n");
									break;
								}
								
								if(newPrice >= 0){
									lineArray[2] = Double.toString(newPrice);
									newLine = lineArray[0] + ":" + lineArray[1] + ":" + lineArray[2];
									fileContent.set(itemLineNumber, newLine);
									outToClient.writeBytes("\r\nPrice updated successfully\r\n");
									outToClient.writeBytes("\r\nNew price: " + lineArray[2] + "\r\n");
								}
								else {
									outToClient.writeBytes("\r\nERROR: Please enter a positive price\r\n");
								}
								
								break;
								
					default: 	outToClient.writeBytes("\r\nERROR: Input a menu option\r\n");
				}
				
				Files.write(path, fileContent, StandardCharsets.UTF_8);

				outToClient.writeBytes("\r\nGo again? (y/n)\r\n");
				
				String yn = inFromClient.readLine();
				if(yn.equals("n")){
					outToClient.writeBytes("\r\nGoodbye\r\n");
					System.out.println("Client disconnected");
					connectedClientSocket.close();
					break;
				}
			}
		}
	}
}
