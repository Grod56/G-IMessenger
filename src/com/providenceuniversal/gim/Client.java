package com.providenceuniversal.gim;

import java.io.*;
import java.net.*;
import java.util.*;

/**
 * The {@code Client} class is half of the core of G-Instant Messenger™ (aside {@code Server} class).
 * It acts as a client, facilitating communication with the server and providing a user-friendly CLI
 * exposing the full functionality of G-Instant Messenger™.
 * 
 * @author Garikai Gumbo<br>
 * Providence Universal Studios®<br>
 */
public class Client {
	private User currentUser;
	private ContactList contacts;
	private ObjectInputStream incomingResponses;
	private ObjectOutputStream outgoingRequests;
	private Scanner keyboardInput;
	
	/**
	 * Creates new client and initializes the client's {@code incomingResponses, outgoingRequests} and
	 * {@code keyboardInput} instance variables respectively, with the corresponding parameter values.
	 * 
	 * @param incomingResponses {@code ObjectInputStream} storing responses coming from server
	 * @param outgoingRequests {@code ObjectOutputStream} storing client requests bound for server
	 * @param keyboardInput {@code Scanner} storing the keyboardInput stream
	 */
	public Client(ObjectInputStream incomingResponses,
			ObjectOutputStream outgoingRequests, Scanner keyboardInput) {
		super();
		this.incomingResponses = incomingResponses;
		this.outgoingRequests = outgoingRequests;
		this.keyboardInput = keyboardInput;
	}

	public static void main(String[] args) {
		
		//Inputting server host address
		Scanner keyboardInput = new Scanner(System.in);
		System.out.println("Please enter the server address (or Computer name)");
		
		//Try-with-resources block setting up the resources to be used by the client
		try (Socket socket = new Socket(keyboardInput.nextLine(), 4279);
				ObjectOutputStream outgoingRequests = new ObjectOutputStream(socket.getOutputStream());
				ObjectInputStream incomingResponses = new ObjectInputStream(socket.getInputStream())) {

			//Client object to operate in non-static contexts
			Client newOnlineClient = new Client(incomingResponses, outgoingRequests, keyboardInput);
			System.out.println("Welcome to G-Instant Messenger\n");
			
			//Loop to cycle through initial options
			while (true) {
				System.out.println("Please enter one of the numerical options to continue:\n"
						+ "1. Login 2. Create new account 3. Delete Account 4. About App 0. Exit");
				
				//String storing option keyed in by user
				String option = keyboardInput.nextLine();

				//Logging in
				if (option.equals("1")) {
					System.out.println("Please enter your username, then your password:");
					//Log into account using input provided by user
					newOnlineClient.login(keyboardInput.nextLine(), keyboardInput.nextLine());
				}
				//Creating account
				else if (option.equals("2")) {
					System.out.println("Please enter the desired username, then your password\n"
					+ "(Note: Username should not be blank and password has to have 4 or more characters. "
					+ "The ' character is illegal)");
					//Create account using input provided by user
					newOnlineClient.signUp(keyboardInput.nextLine(), keyboardInput.nextLine());
				}
				//Deleting account
				else if (option.equals("3")) {
					System.out.println("Please enter the desired username, then your password");
					//Delete account using input provided by user
					newOnlineClient.deleteAccount(keyboardInput.nextLine(), keyboardInput.nextLine());
				}
				//Viewing application "About" information
				else if (option.equals("4")) {
					System.out.println("\nG-Instant Messenger (TM)\n"
							+ "Developed by Providence Universal Studios (R)\n" + 
							"Copyright (c) 2018\n" + 
							"All rights reserved\n");
				}
				//Exiting the system
				else if (option.equals("0")) {
					System.out.println("Exiting the application ...");
					socket.close();
					incomingResponses.close();
					outgoingRequests.close();
					keyboardInput.close();
					try {
						Thread.sleep(3000);
					}
					catch (InterruptedException e) {}
					System.exit(0);
				}
				//In case user enters an invalid option
				else {
					System.out.println("Invalid selection, please try again.\n");
				}
			}
		}
		//Terminating the client in case the client fails to connect to the server or IO errors occur in body
		catch (IOException ex) {
			System.err.println("Failed to connect to G-Instant Messenger network: (" + ex.getMessage() + ")");
			try {
				Thread.sleep(3000);
			}
			catch (InterruptedException e) {}
			System.exit(1);
		} 
	}
	
	//Method to log user in
	public void login(String username, String password) throws IOException{
		
		int timeoutCounter = 0;
		//Loop to retry logging user in with provided credentials in case there are network problems
		while (true) {
			try {
				//Creating the login request and declaring variable to store subsequent response from server
				ClientMessage credentials = new Authentication(username.trim(),
				password.trim(), Authentication.Type.LOGIN);
				ServerMessage response;

				outgoingRequests.writeObject(credentials);
				outgoingRequests.flush();

				System.out.println("Logging in ...\n");

				//Assigning response from server to response variable
				response = (ServerMessage) incomingResponses.readObject();

				//If login is successful
				if (response instanceof User) {
					/*Assigning User object obtained from server to client's currentUser instance variable
					 *and entering the main menu
					 */
					currentUser = (User) response;
					enterMainMenu();
				}
				//If login is unsuccessful
				else {
					ServerError error = (ServerError) response;
					System.out.println(error + "\n");
				}
				break;
			}
			catch (IOException | ClassNotFoundException ex) {
				/*Incrementing the timeout counter in case 
				 *there is an error communicating with server
				 */
				if (timeoutCounter <= 3) {
					timeoutCounter++;
					try {
						Thread.sleep(3000);
					}
					catch (InterruptedException e) {}
				}
				else {
					//Throwing new IOException if the timeout has expired
					throw new IOException(ex);
				}
			} 
		}
	}

	//Method to create account
	public void signUp(String username, String password) throws IOException{
		
		//If statement to sanity check credentials
		if (!username.trim().isEmpty() && password.trim().length() >= 4 && !username.contains("'") && !password.contains("'")) {
			int timeoutCounter = 0;
			//Loop to retry signing user up using provided credentials in case there are network problems
			while (true) {
				try {
					//Creating the signup request and declaring variable to store subsequent response from server
					ClientMessage credentials = new Authentication(username.trim(), password.trim(),
					Authentication.Type.ACCOUNT_CREATION);
					ServerMessage response;

					outgoingRequests.writeObject(credentials);
					outgoingRequests.flush();

					System.out.println("Signing up ...\n");

					//Assigning response from server to response variable
					response = (ServerMessage) incomingResponses.readObject();

					//If sign up is successful
					if (response instanceof User) {
						/*Assigning User object obtained from server to client's currentUser instance variable
						 *and entering the main menu
						 */
						currentUser = (User) response;
						enterMainMenu();
					}
					//If signup is unsuccessful
					else {
						ServerError error = (ServerError) response;
						System.out.println(error + "\n");
					}
					break;
				} catch (IOException | ClassNotFoundException ex) {
					/*Incrementing the timeout counter in case 
					 *there is an error communicating with server
					 */
					if (timeoutCounter <= 3) {
						timeoutCounter++;
						try {
							Thread.sleep(3000);
						} catch (InterruptedException e) {
						}
					} else {
						//Throwing new IOException if the timeout has expired
						throw new IOException(ex);
					}
				}
			} 
		}
		//In case credentials fail sanity check
		else {
			System.out.println("Your credentials do not match the"
			+ " minimum required criteria or contain illegal characters, check your input and try again.\n");
		}
	}
	
	//Method to delete account
	public void deleteAccount(String username, String password) throws IOException{
		
		int timeoutCounter = 0;
		
		//Loop to retry deleting user using provided credentials in case there are network problems
		while (true) {
			try {
				//Creating the deletion request and declaring variable to store subsequent response from server
				ClientMessage credentials = new Authentication(username,
				password, Authentication.Type.ACCOUNT_DELETION);
				ServerMessage response;

				outgoingRequests.writeObject(credentials);
				outgoingRequests.flush();

				System.out.println("Deleting account ...\n");

				//Assigning response from server to response variable
				response = (ServerMessage) incomingResponses.readObject();

				//If deletion is successful
				if (response instanceof CommitMessage) {
					System.out.println((CommitMessage) response + "\n");
				}
				//If deletion is unsuccessful
				else {
					ServerError error = (ServerError) response;
					System.out.println(error + "\n");
				}
				break;
			}
			catch (IOException | ClassNotFoundException ex) {
				/*Incrementing the timeout counter in case 
				 *there is an error communicating with server
				 */
				if (timeoutCounter <= 3) {
					timeoutCounter++;
					try {
						Thread.sleep(3000);
					}
					catch (InterruptedException e) {}
				}
				else {
					//Throwing new IOException if the timeout has expired
					throw new IOException(ex);
				}
			} 
		}
	}
	
	//Method to expose main menu options to user
	private void enterMainMenu() throws IOException {
		
		//Loop to cycle through menu options multiple times
		while (true) {
			System.out.println("Welcome " + currentUser.getUsername() + 
					", please enter one of the numerical options to continue:\n"
					+ "1. Send Message 2. View Contacts 3. Open Chat 4. Logout");
			
			//String storing option keyed in by user
			String option = keyboardInput.nextLine();
			
			//Try-catch block to handle any input parsing errors
			try {
				//Sending Message
				if (option.equals("1")) {
					//Update contact list object stored by client and display the contacts
					retrieveContacts();
					System.out.println("Enter the number corresponding with the desired contact:");
					System.out.println(contacts + "\n" + "0. Cancel");

					//Read input
					int selection = Integer.parseInt(keyboardInput.nextLine());
					if (selection != 0) {
						//Sanity checking the input
						if (selection > 0 && selection <= contacts.getTotalNumberOfContacts()) {
							System.out.println("Now type in the message you want to send:");
							String messageBody = keyboardInput.nextLine();
							sendMessage(selection, messageBody); //Send the message
						}
						//In case input is invalid
						else {
							System.out.println("You have entered an invalid option, please try again.\n");
						} 
					}
				}
				//View contacts
				else if (option.equals("2")) {
					//Update contact list object stored by client and display the contacts
					retrieveContacts();
					System.out.println(contacts + "\n");
				}
				//Open chat
				else if (option.equals("3")) {
					//Update contact list object stored by client and display the contacts
					retrieveContacts();
					System.out.println("Enter the number corresponding with the desired contact:");
					System.out.println(contacts + "\n" + "0. Cancel");
					
					//Read input
					int selection = Integer.parseInt(keyboardInput.nextLine());
					if (selection != 0) {
						//Sanity checking input
						if (selection > 0 && selection <= contacts.getTotalNumberOfContacts()) {
							retrieveChatHistory(selection); //Retrieve Chats 
							System.out.println();
						}
						//In case input is invalid
						else {
							System.out.println("You have entered an invalid option, please try again.\n");
						} 
					}
				}
				//Log out
				else if (option.equals("4")) {
					logout(); //Log out user
					break;
				}
				//In case selection is invalid
				else {
					System.out.println("Invalid selection, please check your input and try again.\n");
				}
			//In case parsing input to a number fails
			} catch (NumberFormatException ex) {
				System.out.println("Numerical input only please, do try again.\n");
			}	
		}
	}
	
	//Method to send message
		private void sendMessage(int recipientOption, String messageBody) throws IOException{
			
			int timeoutCounter = 0;
			//Loop to retry sending message user using provided credentials in case there are network problems
			while (true) {
				try {
					/*Creating the send message request and declaring 
					 *variable to store subsequent response from server
					 */
					ClientMessage chatMessage = new ChatMessage(currentUser.getUsername(),
					contacts.getContactName(recipientOption),
					messageBody.replace("'", "''"));
					ServerMessage response;
					
					outgoingRequests.writeObject(chatMessage);
					outgoingRequests.flush();
					
					System.out.println("Sendinng message ...\n");
					
					//Assigning response from server to response variable
					response = (ServerMessage) incomingResponses.readObject();
					
					//If message successfully sent
					if (response instanceof CommitMessage) {
						System.out.println((CommitMessage) response + "\n");
					}
					//If message not sent
					else {
						ServerError error = (ServerError) response;
						System.out.println(error + "\n");
					}
					break;
				}
				catch (IOException | ClassNotFoundException ex) {
					/*Incrementing the timeout counter in case 
					 *there is an error communicating with server
					 */
					if (timeoutCounter <= 3) {
						timeoutCounter++;
						try {
							Thread.sleep(3000);
						}
						catch (InterruptedException e) {}
					}
					else {
						//Throwing new IOException if the timeout has expired
						throw new IOException(ex);
					}
				}
			}
		}
	
	//Method to retrieve chat history
	private void retrieveChatHistory(int recipientOption) throws IOException{
		
		int timeoutCounter = 0;
		//Loop to retry retrieving chats in case there are network problems
		while (true) {
			try {
				/*Creating the chat history request and declaring
				 *variable to store subsequent response from server
				 */
				ClientMessage chatsRequest = new ChatHistoryRequest(currentUser.getUsername(),
				contacts.getContactName(recipientOption));
				ServerMessage response;
				
				outgoingRequests.writeObject(chatsRequest);
				outgoingRequests.flush();
				//Assigning response from server to response variable
				response = (ServerMessage) incomingResponses.readObject();
				
				//If chats request is successful
				if (response instanceof ChatHistory) {
					ChatHistory chatHistory = (ChatHistory) response;
					System.out.println(chatHistory);
				}
				//If chats request is unsuccessful
				else {
					ServerError error = (ServerError) response;
					System.out.println(error + "\n");
				}
				break;
			}
			catch (IOException | ClassNotFoundException ex) {
				/*Incrementing the timeout counter in case 
				 *there is an error communicating with server
				 */
				if (timeoutCounter <= 3) {
					timeoutCounter++;
					try {
						Thread.sleep(3000);
					}
					catch (InterruptedException e) {}
				}
				else {
					//Throwing new IOException if the timeout has expired
					throw new IOException(ex);
				}
			}
		}
	}
	
	//Method to retrieve contacts
	private void retrieveContacts() throws IOException {
		
		int timeoutCounter = 0;
		//Loop to retry retrieving contacts in case there are network problems
		while (true) {
			try {
				//Creating the contacts request and declaring variable to store subsequent response from server
				ClientMessage contactsRequest = new ContactsRequest();
				ServerMessage response;
				
				outgoingRequests.writeObject(contactsRequest);
				outgoingRequests.flush();
				//Assigning response from server to response variable
				response = (ServerMessage) incomingResponses.readObject();
				
				//If contacts request is successful
				if (response instanceof ContactList) {
					contacts = (ContactList) response;
				}
				//If contacts request is unsuccessful
				else {
					ServerError error = (ServerError) response;
					System.out.println(error + "\n");
				}
				break;
			}
			catch(IOException | ClassNotFoundException ex) {
				/*Incrementing the timeout counter in case 
				 *there is an error communicating with server
				 */
				if (timeoutCounter <= 3) {
					timeoutCounter++;
					try {
						Thread.sleep(3000);
					}
					catch (InterruptedException e) {}
				}
				else {
					//Throwing new IOException if the timeout has expired
					throw new IOException(ex);
				}
			}
		}
	}
	
	//Method to log user out
	private void logout() throws IOException{
		
		int timeoutCounter =  0;
		while (true) {
			try {
				//Creating the disconnect request and declaring variable to store subsequent response from server
				ClientMessage disconnectRequest = new UserDisconnection();
				ServerMessage response;
				
				outgoingRequests.writeObject(disconnectRequest);
				outgoingRequests.flush();
				//Assigning response from server to response variable
				response = (ServerMessage) incomingResponses.readObject();
				
				//If log out is successful
				if (response instanceof CommitMessage) {
					System.out.println((CommitMessage) response + "\n");
					currentUser = null;
					contacts = null;
				}
				//If logout is unsuccessful
				else {
					ServerError error = (ServerError) response;
					System.out.println(error + "\n");
				}
				break;
			}
			catch (IOException | ClassNotFoundException ex) {
				/*Incrementing the timeout counter in case 
				 *there is an error communicating with server
				 */
				if (timeoutCounter <= 3) {
					timeoutCounter++;
					try {
						Thread.sleep(3000);
					}
					catch (InterruptedException e) {}
				}
				else {
					//Throwing new IOException if the timeout has expired
					throw new IOException(ex);
				}
			} 
		}
	}
}
