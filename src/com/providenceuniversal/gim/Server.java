package com.providenceuniversal.gim;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import storage.Database;

/**
 * The {@code Server} class is half of the core of G-Instant Messenger™ (aside {@code Client} class).
 * It acts as a server, facilitating handling of multiple clients concurrently and storing messages and
 * user information in a database.
 * 
 * @author Garikai Gumbo<br>
 * Providence Universal Studios®
 */
public class Server{
	
	//_________________________________Server Initialization and Startup_____________________________________
	
	//Static variables to store users logged in and logged out of the system (respectively)
	private static HashMap<String, Socket> onlineUsers;
	private static HashMap<String, LocalDateTime> offlineUsers;
	private static FileWriter logFileWriter;
	private static Database persistence;
	
	//Static initializer to instantiate the server's static variables
	static {
		onlineUsers = new HashMap<String, Socket>();
		offlineUsers = new HashMap<String, LocalDateTime>();
	}
	
	public static void main(String[] args) {
		
		//Creating new file object referencing the location of the relevant log file
		File logFile = new File(System.getProperty("user.home") + "/G-Instant Messenger/logs/logFile.log");
		
		//Creating the log file in case it does not exist
		if (!logFile.exists())
			logFile.getParentFile().mkdirs();
		
		//Try-with-resources block setting up the resources to be used by the server
		try(ServerSocket serverSocket = new ServerSocket(4279);
			Database persistence = new Database("g_im.db");){
			
			//Referencing static resources to the local instances
			FileWriter logFileWriter = new FileWriter(logFile, true);
			Server.logFileWriter = logFileWriter;
			Server.persistence = persistence;
			
			//Executor service which shall be used to handle multiple clients
			ExecutorService executor = Executors.newCachedThreadPool();
			
			//Table of results obtained from database containing all users of G-Instant Messenger
			ResultSet users = persistence.retrieveRecords(new String[] {"Users"}, true);
			
			//Loop to fill the offline users collection with the users ResultSet
			while(users.next()) {
				offlineUsers.put(users.getString(1), Timestamp.valueOf(users.getString(3)).toLocalDateTime());
			}
			
			//Logging initial messages
			logInformation("Server running on '" +
			InetAddress.getLocalHost().getHostName() + "' and listening on port "
			+ serverSocket.getLocalPort(), true);
			logInformation("Waiting for client connections ... " + System.lineSeparator(), false);
			
			//Loop to listen for any client connections
			while (true) {
				Socket clientSocket = serverSocket.accept();
				
				//Creation and execution of separate handler thread upon connection
				Runnable handlerThread = new Server.ClientRequestHandler(clientSocket);
				executor.execute(handlerThread);
				//Logging client connection
				logInformation("Client, " + clientSocket.getInetAddress().getHostName() +
				" (" + clientSocket.getInetAddress().getHostAddress() + ")," +
				" has connected to the server.", false);
				
			}
		}
		//Terminate server in case there is an error communicating with server resources
		catch(IOException | SQLException ex) {
			
			//Logging the terminating exception
			try {
				logInformation("Fatal server error: " + ex, false);
			}
			catch (IOException e) {
				e.printStackTrace();
			}
		}
		finally {
			try {
				logFileWriter.close();
			}
			catch (IOException e) {}
		}
	}
	
	//Method logging information to display and log file
	static void logInformation(String logEntry, boolean returnBeforeLogging) throws IOException {
		String timestampedLogEntry = LocalDateTime.now().toString() + "> " + logEntry;
		System.out.println(timestampedLogEntry);
		logFileWriter.write((returnBeforeLogging ? System.lineSeparator() : "") +
		timestampedLogEntry + System.lineSeparator());
		logFileWriter.flush();
	}
	
	//______________________________________Handling of individual clients_____________________________________
	
	/**
	 * The {@code ClientRequestHandler} nested class handles requests from a given client parallel to
	 * the running of the server
	 * @author Garikai Gumbo<br>
	 * Providence Universal Studios®<br>
	 */
	private static class ClientRequestHandler implements Runnable{
		
		//Instance variables for given client handler
		private Socket clientSocket;
		private String currentUser;
		
		//Constructor assigning the handler's clientSocket reference
		ClientRequestHandler(Socket clientSocket) throws SQLException{
			this.clientSocket = clientSocket;
		}
		
		@Override
		public void run() {
			//Try-with-resources block setting up the resources to be used by the handler
			try(ObjectOutputStream outgoingResponse = new ObjectOutputStream(clientSocket.getOutputStream());
				ObjectInputStream incomingRequest = new ObjectInputStream(clientSocket.getInputStream());){
				
				int timeoutCounter = 0;
				//Loop to listen for client requests
				while(true) {
					try {
						ClientMessage request = (ClientMessage) incomingRequest.readObject();
						outgoingResponse.writeObject(handleRequest(request));
						outgoingResponse.flush();
					}
					catch (IOException | ClassNotFoundException ex) {
						/*Incrementing the timeout counter in case 
						 *there is an error communicating with client socket
						 */
						if (timeoutCounter <= 3) {
							timeoutCounter++;
							try {
								Thread.sleep(3000);
							}
							catch (InterruptedException e) {}
						}
						//Throwing new IOException if the timeout has expired
						else {
							throw new IOException();
						}
					}
				}
			}
			/*Disconnecting the client in case there are errors
			 *communicating with the client handler resources
			 */
			catch(IOException ex) {
				disconnectClient();
			}
		}

		//Method to handle a particular client request
		private ServerMessage handleRequest(ClientMessage request) {
			
			//If-else if block to determine the request type
			if (request instanceof Authentication) { //Executes if the client request is Authentication
				Authentication authenticationRequest = (Authentication) request;
				
				//If-else if block to determine Authentication type
				
				//Executes if account creation
				if (authenticationRequest.getAuthenticationType().equals(Authentication.Type.ACCOUNT_CREATION)) {
					return createAccount(authenticationRequest);
				}
				//Executes if login
				else if (authenticationRequest.getAuthenticationType().equals(Authentication.Type.LOGIN)) {
					return login(authenticationRequest);
				}
				//Executes if account deletion
				else{
					return deleteAccount(authenticationRequest);
				}
			}
			//Executes if the client request is ChatMessage
			else if(request instanceof ChatMessage){
				return sendChat((ChatMessage) request);
			}
			//Executes if the client request is UserDisconnection
			else if(request instanceof UserDisconnection) {
				return disconnectUser((UserDisconnection) request);
			}
			//Executes if the client request is ChatHistoryRequest
			else if (request instanceof ChatHistoryRequest) {
				return retrieveChats((ChatHistoryRequest) request);
			}
			//Executes if the client request is ContactRequest
			else if (request instanceof ContactsRequest) {
				return retrieveContacts((ContactsRequest) request);
			}
			//Executes in case request type is invalid
			else {
				return new ServerError("Invalid request type");
			}
		}

		//Method retrieving contacts per ContactRequest
		private ServerMessage retrieveContacts(ContactsRequest request) {
			HashMap<String, String> contactsMap = new HashMap<String, String>();
			
			//Populating the contactsMap with contacts stored in the server static variables
			for (String onlineUser: onlineUsers.keySet()) {
				contactsMap.put(onlineUser, "Online");
			}	
			for (String offlineUser: offlineUsers.keySet()){
				contactsMap.put(offlineUser,"Last seen " + offlineUsers
						.get(offlineUser)
						.format(DateTimeFormatter
						.ofPattern("dd MMMM yy, HH:mm")).toString());
			}
			contactsMap.remove(currentUser);
			return new ContactList(contactsMap); //Returning new ContactList object
		}

		//Method retrieving chats per ChatHistoryRequest
		private ServerMessage retrieveChats(ChatHistoryRequest request) {
			try {
				
				//Table of results storing chats between the two participants in the request
				ResultSet chatsQuery = persistence.retrieveRecords(new String[] {"Chat_Messages"}, 
				"(Sender = '" + request.getParticipant1() + "' AND Receiver = '" + request.getParticipant2() +
				"') OR (Receiver = '" + request.getParticipant1() + "'AND Sender = '" +
				request.getParticipant2() + "')", true, new String[]{"Timestamp"});
				
				return new ChatHistory(chatsQuery); //Returning new ChatHistory object 
			}
			//Returning an error response in case there is failure communicating with the database
			catch(SQLException ex) {
				//Returning an error in case there is failure communicating with the database
				try {
					logInformation("Error at client " + clientSocket.getLocalAddress() + ": " + ex, false);
				} catch (IOException e) {
					System.err.println("Failed to write to log file: " + e);
				}
				return new ServerError("Unable to retrieve your chats: "
						+ "There was an error connecting to the G-Instant Messenger database");
			}
		}

		//Method disconnecting/logging out user from network
		private ServerMessage disconnectUser(UserDisconnection request) {
			try {
				//Updating log out time of user in database
				persistence.updateRecord("Users", new String[] {"Last_Seen"}, new String[] {
				Timestamp.valueOf(request.getDisconnectionTime()).toString()},
				"Username = '" + currentUser + "'");
				
				//Updating server contact lists
				onlineUsers.remove(currentUser);
				offlineUsers.put(currentUser, request.getDisconnectionTime());
				
				//Logging the user disconnection
				try {
					logInformation("User, " + currentUser + ", has logged off client " 
					+ clientSocket.getInetAddress(), false);
				}
				catch (IOException ex) {
					System.err.println("Failed to write to log file: " + ex);
				}
				
				currentUser = null;
				
				//Returning confirmation of success
				return new CommitMessage("Successfully logged you out of the network");
			}
			//Returning an error response in case there is failure communicating with the database
			catch (SQLException ex) {
				//Returning an error in case there is failure communicating with the database
				try {
					logInformation("Error at client " + clientSocket.getLocalAddress() + ": " + ex, false);
				} catch (IOException e) {
					System.err.println("Failed to write to log file: " + e);
				}
				return new ServerError("Unable to log you off the server: "
						+ "There was an error connecting to the G-Instant Messenger database");
			}
		}

		//Method sending chat message to specific user per ChatMessage request
		private ServerMessage sendChat(ChatMessage request) {
			try {
				//Adding the chat message to the database
				persistence.addRecord("Chat_Messages", Integer.toString(request.hashCode()), request.getSender(),
				request.getRecipient(), request.getBody(), Timestamp.valueOf(request.getTimeStamp()).toString());
				
				//Returning confirmation of success
				return new CommitMessage("Message was successfully sent.");
			}
			//Returning an error response in case there is failure communicating with the database
			catch(SQLException ex) {
				//Returning an error in case there is failure communicating with the database
				try {
					logInformation("Error at client " + clientSocket.getLocalAddress() + ": " + ex, false);
				} catch (IOException e) {
					System.err.println("Failed to write to log file: " + e);
				}
				return new ServerError("Unable to send your message: "
						+ "There was an error connecting to the G-Instant Messenger database");
			}
			
		}

		//Method deleting account as specified in the Authentication request credentials
		private ServerMessage deleteAccount(Authentication request) {
			try {
				//Table of results to store the user retrieved from database matching credentials in request
				ResultSet matches = persistence.retrieveRecords(new String[] {"Users"}, "Username = '" +
				request.getUsername() +"' AND  Password = '" + request.getPassword() + "'", false);
				
				//Deleting user from database and updating server contact lists in case there are matches
				if (matches.next()) {
					persistence.deleteRecords("Users", "Username = '" + request.getUsername() +
					"' AND  Password = '" + request.getPassword() + "'");
					onlineUsers.remove(request.getUsername());
					offlineUsers.remove(request.getUsername());
					
					//Logging the account deletion
					try {
						logInformation("User, " + request.getUsername() +
						", has terminated their account", false);
					}
					catch (IOException ex) {
						System.err.println("Failed to write to log file: " + ex);
					}
					
					//Returning confirmation of success
					return new CommitMessage("Successfully deleted the user.");
				}
				//Returning an error response in case the credentials don't match any records
				else {
					return new ServerError("The credentials you entered are invalid.");
				}
			//Returning an error in case there is failure communicating with the database
			} catch (SQLException ex) {
				//Returning an error in case there is failure communicating with the database
				try {
					logInformation("Error at client " + clientSocket.getLocalAddress() + ": " + ex, false);
				} catch (IOException e) {
					System.err.println("Failed to write to log file: " + e);
				}
				return new ServerError("Unable to delete your account: "
						+ "There was an error connecting to the G-Instant Messenger database");
			}
		}

		//Method logging account in as specified in the Authentication request credentials
		private ServerMessage login(Authentication request) {
			try {
				//Table of results to store the user retrieved from database matching credentials in request
				ResultSet matches = persistence.retrieveRecords(new String[] {"Users"},
				"Username = '" + request.getUsername() +
				"' AND Password = '" + request.getPassword() + "'", false);
				
				/*Updating server contact lists and reassigning handler's
				 *currentUser instance variable in case there are matches
				 */
				if (matches.next()) {
					onlineUsers.put(matches.getString(1), clientSocket);
					offlineUsers.remove(matches.getString(1));
					currentUser = new User(matches.getString(1)).getUsername();
					
					//Logging the login
					try {
						logInformation("User, " + currentUser + ", has logged into client, " +
						clientSocket.getInetAddress(), false);
					}
					catch (IOException ex) {
						System.err.println("Failed to write to log file: " + ex);
					}
					
					return new User(currentUser); //Returning user object
				}
				else {
					//Returning an error response in case the credentials don't match any records
					return new ServerError("The credentials you entered are invalid.");
				}
			} catch (SQLException ex) {
				//Returning an error in case there is failure communicating with the database
				try {
					logInformation("Error at client " + clientSocket.getLocalAddress() + ": " + ex, false);
				} catch (IOException e) {
					System.err.println("Failed to write to log file: " + e);
				}
				return new ServerError("Unable to log you in: "
						+ "There was an error connecting to the G-Instant Messenger database");
			}
		}

		//Method creating user as specified in the Authentication request credentials
		private ServerMessage createAccount(Authentication request) {
			try {					
				//Adding user to database and onlineUsers list
				persistence.addRecord("Users", new String[] {request.getUsername(), request.getPassword(),
				Timestamp.valueOf(LocalDateTime.now()).toString()});
				onlineUsers.put(request.getUsername(), clientSocket);
				
				//Assigning user to handler's current user instance variable
				currentUser = request.getUsername();
				
				//Logging the sign up
				try {
					logInformation("User, " + currentUser + 
					", has just signed up and logged in at " + clientSocket.getInetAddress(), false);
				}
				catch (IOException ex) {
					System.err.println("Failed to write to log file: " + ex);
				}
				
				return new User(currentUser);
			}
			//Returning an error in case there is failure communicating with the database
			catch (SQLException ex) {
				//Returning an error in case there is failure communicating with the database
				try {
					logInformation("Error at client " + clientSocket.getLocalAddress() + ": " + ex, false);
				} catch (IOException e) {
					System.err.println("Failed to write to log file: " + e);
				}
				return new ServerError("Unable to create your account: "
						+ "There was an error connecting to the G-Instant Messenger database");
			}
		}
		
		//Method disconnecting the client
		private void disconnectClient() {
			//Logging user out first in case the client terminated with an account logged in
			if (currentUser != null) {
				ServerMessage userDisconnectResponse = disconnectUser(new UserDisconnection());
				if(userDisconnectResponse instanceof ServerError) {
					onlineUsers.remove(currentUser);
					offlineUsers.put(currentUser, LocalDateTime.now());
					currentUser = null;
				}
			}
			//Logging the disconnection
			try {
				logInformation("Client, " + clientSocket.getInetAddress() +
				", has disconnected from server.", false);
			}
			catch (IOException ex) {
				System.err.println("Failed to write to log file: " + ex);
			}
		}
		
	}
}
