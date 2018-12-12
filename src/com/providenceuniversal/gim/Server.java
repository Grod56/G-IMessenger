package com.providenceuniversal.gim;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Scanner;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.TimeUnit;

import storage.Database;

/**
 * The {@code Server} class is half of the core of G-Instant Messenger (aside {@code Client} class).
 * It acts as a server, facilitating handling of multiple clients concurrently and storing messages and
 * user information in a database.
 * 
 * @author Garikai Gumbo<br>
 * Providence Universal Studios�
 */
public class Server{
	
	//_________________________________Server Initialization and Startup_____________________________________
	
	//Static variables to store users logged in and logged out of the system (respectively)
	private static ServerSocket serverSocket;
	private static FileWriter logFileWriter;
	private static Database database;
	private static HashMap<String, ClientRequestHandler> onlineUsers;
	private static HashMap<String, LocalDateTime> offlineUsers;
	private static volatile ExecutorService commandExecutor, clientsExecutor,
											listenersExecutor, notificationsExecutor;
	
	//Static initializer to instantiate the server's static variables and initiate logger
	static {
		onlineUsers = new HashMap<String, ClientRequestHandler>();
		offlineUsers = new HashMap<String, LocalDateTime>();
		commandExecutor = Executors.newSingleThreadExecutor();
		clientsExecutor = Executors.newCachedThreadPool();
		listenersExecutor = Executors.newCachedThreadPool();
		notificationsExecutor = Executors.newCachedThreadPool();
		
		//Creating new file object referencing the location of the relevant log file
		File logFile = new File(System.getProperty("user.home") + "/G-Instant Messenger/logs/logFile.log");
		//Creating the log file in case it does not exist
		if (!logFile.exists())
			logFile.getParentFile().mkdirs();
		try {
			FileWriter logFileWriter = new FileWriter(logFile, true);
			Server.logFileWriter = logFileWriter;
			commandExecutor.execute(() -> commandListener());
		}
		catch (IOException ex) {
			System.err.println("Fatal server error: (" + ex + ")");
			System.exit(1);
		}
	}
	
	public static void main(String[] args) {
		
		//Try-with-resources block setting up the resources to be used by the server
		try(ServerSocket serverSocket = new ServerSocket(4279);
			Database database = new Database("g_im.db");){
			
			//Referencing static resources to the local instances
			Server.serverSocket = serverSocket;
			Server.database = database;
			
			//Table of results obtained from database containing all users of G-Instant Messenger
			ResultSet users = database.retrieveRecords(new String[] {"Users"}, true);
			
			//Loop to fill the offline users collection with the users ResultSet
			while(users.next()) {
				offlineUsers.put(users.getString(1), Timestamp.valueOf(users.getString(3)).toLocalDateTime());
			}
			users.close();
			//Logging initial messages
			logInformation("Server running on '" + InetAddress.getLocalHost().getHostName() +
			"' (" + InetAddress.getLocalHost().getHostAddress() + ") and listening on port " +
			serverSocket.getLocalPort(), true);
			logInformation("Waiting for client connections ... " + System.lineSeparator(), false);
			
			//Loop to listen for any client connections
			while (true) {
				try {
					Socket handlerSocket = serverSocket.accept();
					//Creation and execution of separate handler thread upon connection
					clientsExecutor.execute(new Server.ClientRequestHandler(handlerSocket));
				}
				catch (SocketException ex) {
					//Throwing exception if exception was not caused by the shutting down of the serverSocket
					if (!serverSocket.isClosed()) {
						throw new IOException(ex);
					}
					else {
						//Logging final information
						logInformation("Server successfully shutdown.", false);
						logFileWriter.close();
						break;
					}
				}
				catch (RejectedExecutionException ex) {}
			}
		}
		/*Terminate server in case there is an error communicating with 
		 *server resources or other sundry server errors
		*/
		catch(IOException | SQLException ex) {
			//Logging the terminating exception
			try {
				logInformation("Fatal server error: " + ex, false);
				if (!commandExecutor.isShutdown()) {
					commandExecutor.shutdownNow();
					commandExecutor.awaitTermination(2, TimeUnit.SECONDS);
				}
			}
			catch (IOException | InterruptedException e) {}
			//Closing the logFileWriter resource
			finally {
				try {
					logFileWriter.close();
				}
				catch (IOException e) {}
			}
			//Exiting the system
			System.exit(1);
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
	
	//Method to listen for any keyboard commands
	static void commandListener() {
		Scanner keyboardInput = new Scanner(System.in);
		while (true) {
			String command = keyboardInput.nextLine().trim();
			//Execute if shutdown command is passed
			if (command.equalsIgnoreCase("shutdown")) {
				initiateShutdownSequence();
				keyboardInput.close();
				
				//Shutting down command executor and serverSocket
				commandExecutor.shutdown();
				try {
					serverSocket.close();
				}
				catch (IOException e) {}
				break;
			}
		}
	}
	
	//Method to shutdown server
	static void initiateShutdownSequence() {
		//Logging the shutdown initiation
		try {
			logInformation("Shutting down server ...", false);
		}
		catch (IOException ex) {
			System.err.println("Failed to write to log file: (" + ex + ")");
		}
		
		//Shutting down all running threads
		try {
			notificationsExecutor.shutdown();
			listenersExecutor.shutdown();
			clientsExecutor.shutdown();
			notificationsExecutor.awaitTermination(5, TimeUnit.SECONDS);
			listenersExecutor.awaitTermination(2, TimeUnit.SECONDS);
			clientsExecutor.awaitTermination(2, TimeUnit.SECONDS);
		}
		//In case the shutdown sequence incurs some errors
		catch (InterruptedException ex) {
			//Logging the errors
			try {
				logInformation("Errors while shutting down server: (" + ex + ")", false);
			}
			catch (IOException e) {
				System.err.println("Failed to write to log file: (" + e + ")");
			}
		}
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
		private final Socket handlerSocket;
		private ObjectOutputStream outgoingServerMessages;
		private String currentUser;
		
		//Constructor assigning the handler's handlerSocket reference
		ClientRequestHandler(Socket handlerSocket) throws SQLException{
			this.handlerSocket = handlerSocket;
			listenersExecutor.execute(() -> checkForShutdownStatus());
		}
		
		@Override
		public void run() {
			//Try-with-resources block setting up the resources to be used by the handler
			try(ObjectOutputStream outgoingResponses = new ObjectOutputStream(handlerSocket.getOutputStream());
				ObjectInputStream incomingRequests = new ObjectInputStream(handlerSocket.getInputStream());){
				
				outgoingServerMessages = outgoingResponses;
				
				//Logging client connection
				try {
					logInformation("Client, " + handlerSocket.getInetAddress().getHostName() +
					" (" + handlerSocket.getInetAddress().getHostAddress() + ")," +
					" has connected to the server.", false);
				}
				catch (IOException ex) {
					System.err.println("Failed to write to log file: (" + ex + ")");
				}
				
				int timeoutCounter = 1;
				//Loop to listen for client requests
				while(true) {
					try {
						ClientMessage request = (ClientMessage) incomingRequests.readObject();
						sendServerMessage(handleRequest(request));
					}
					catch (IOException | ClassNotFoundException ex) {
						/*Incrementing the timeout counter in case 
						 *there is an error communicating with client socket
						 */
						if (timeoutCounter < 3) {
							timeoutCounter++;
							try {
								if (!clientsExecutor.isShutdown()) {
									Thread.sleep(3000);
								}
								else {
									break;
								}
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
			//In case there is an error setting up resources or timeout expires
			catch(IOException ex) {
				/*Disconnecting the client
				 *
				 */
				disconnectClient();
			}
		}

		//Method to check if shutdown sequence is in progress
		private void checkForShutdownStatus() {
			while(true) {
				//Checking if the client has been disconnected
				if (!handlerSocket.isClosed()) {
					if (listenersExecutor.isShutdown()) {
						disconnectClient();
					}
				}
				//Break out of the loop if the client has been disconnected
				else {
					break;
				}
				try {
					Thread.sleep(1);
				}
				catch (InterruptedException e) {}
			}
		}

		//Method to handle a particular client request
		private ServerMessage handleRequest(ClientMessage request) {
			
			//If-else if block to determine the request type
			if (request instanceof Authentication) { //Executes if the client request is Authentication
				Authentication authenticationRequest = (Authentication) request;
				
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
				ResultSet chatsQuery = database.retrieveRecords(new String[] { "Chat_Messages" },
				"(Sender = '" + request.getParticipant1() + "' AND Receiver = '" + request.getParticipant2()
				+ "') OR (Receiver = '" + request.getParticipant1() + "'AND Sender = '"
				+ request.getParticipant2() + "')", true, new String[] { "Timestamp" });

				return new ChatHistory(chatsQuery); //Returning new ChatHistory object 
			}
			//Returning an error response in case there is failure communicating with the database
			catch (SQLException ex) {
				//Logging the exception
				try {
					logInformation("Error at handler for client " + handlerSocket.getInetAddress() +
							": " + ex, false);
				}
				catch (IOException e) {
					System.err.println("Failed to write to log file: " + e);
				}
				return new ServerError("Unable to retrieve your chats: "
						+ "There was an error communicating with the G-Instant Messenger database");
			}
		}

		//Method disconnecting/logging out user from network
		private ServerMessage disconnectUser(UserDisconnection request) {
			try {
				//Updating log out time of user in database
				database.updateRecord("Users", new String[] { "Last_Seen" },
						new String[] { Timestamp.valueOf(request.getDisconnectionTime()).toString() },
						"Username = '" + currentUser + "'");

				//Updating server contact lists
				onlineUsers.remove(currentUser);
				offlineUsers.put(currentUser, request.getDisconnectionTime());

				//Logging the user disconnection
				try {
					logInformation("User, " + currentUser + ", has logged off client " +
					handlerSocket.getInetAddress(), false);
				}
				catch (IOException ex) {
					System.err.println("Failed to write to log file: (" + ex + ")");
				}

				//Notifying all clients that the user has logged off
				String offlineUser = currentUser;
				if (!notificationsExecutor.isShutdown())
					notificationsExecutor.execute(() -> sendNotification(
							new ServerNotification("User '" + offlineUser + "', is now offline.")));
				currentUser = null;
				//Returning confirmation of success
				return new CommitMessage("Successfully logged you out of the network");
			}
			//Returning an error response in case there is failure communicating with the database
			catch (SQLException ex) {
				//Logging the exception
				try {
					logInformation("Error at handler for client " + handlerSocket.getInetAddress() +
							": " + ex, false);
				}
				catch (IOException e) {
					System.err.println("Failed to write to log file: " + e);
				}
				return new ServerError("Unable to log you off the server: "
						+ "There was an error communicating with the G-Instant Messenger database");
			}
		}

		//Method sending chat message to specific user per ChatMessage request
		private ServerMessage sendChat(ChatMessage request) {
			try {
				//Adding the chat message to the database
				database.addRecord("Chat_Messages", Integer.toString(request.hashCode()),
						request.getSender(), request.getRecipient(),
						request.getBody().replace("'","''"),
						Timestamp.valueOf(request.getTimeStamp()).toString());

				//Notifying recipient of new message
				if (!notificationsExecutor.isShutdown())
					notificationsExecutor.execute(() -> sendNotification(request));
				//Returning confirmation of success
				return new CommitMessage("Message was successfully sent.");
			}
			//Returning an error response in case there is failure communicating with the database
			catch (SQLException ex) {
				//Logging the exception
				try {
					logInformation("Error at handler for client " + handlerSocket.getInetAddress() +
							": " + ex, false);
				}
				catch (IOException e) {
					System.err.println("Failed to write to log file: " + e);
				}
				return new ServerError("Unable to send your message: "
						+ "There was an error communicating with the G-Instant Messenger database");
			} 
		}

		//Method deleting account as specified in the Authentication request credentials
		private ServerMessage deleteAccount(Authentication request) {
			try {
				//Checking if account is already logged in on another client
				if (!onlineUsers.containsKey(database.retrieveRecords(new String[] {"Users"}, "Username = '" +
					request.getUsername() + "'", false).getString(1))) {
					
					//Table of results to store the user retrieved from database matching credentials in request
					ResultSet matches = database.retrieveRecords(new String[] { "Users" }, "Username = '"
					+ request.getUsername() + "' AND  Password = '" + request.getPassword() + "'", false);

					//Deleting user from database and updating server contact lists in case there are matches
					if (matches.next()) {
						database.deleteRecords("Users", "Username = '" + request.getUsername()
						+ "' AND  Password = '" + request.getPassword() + "'");
						onlineUsers.remove(request.getUsername());
						offlineUsers.remove(request.getUsername());
						
						//Logging the account deletion
						try {
							logInformation("User, " + request.getUsername() +
									", has terminated their account", false);
						}
						catch (IOException ex) {
							System.err.println("Failed to write to log file: (" + ex + ")");
						}

						//Notifying all clients that the user has deleted their account
						if (!notificationsExecutor.isShutdown())
							notificationsExecutor.execute(() -> sendNotification(new ServerNotification("User '" +
							request.getUsername() + "', has deleted their account")));
						//Returning confirmation of success
						return new CommitMessage("Successfully deleted the user.");
					}
					//Returning an error response in case the credentials don't match any records
					else {
						return new ServerError("The credentials you entered are invalid.");
					}
				}
				else {
					//Returning an error response in case the account is logged in on another client
					return new ServerError("Unable to delete user; the user is logged in on another client.");
				}
			}
			//Returning an error response in case there is failure communicating with the database
			catch (SQLException ex) {
				//Logging the exception
				try {
					logInformation("Error at handler for client " + handlerSocket.getInetAddress() +
							": " + ex, false);
				}
				catch (IOException e) {
					System.err.println("Failed to write to log file: " + e);
				}
				return new ServerError("Unable to delete your account: "
						+ "There was an error communicating with the G-Instant Messenger database");
			} 
		}

		//Method logging account in as specified in the Authentication request credentials
		private ServerMessage login(Authentication request) {
			try {
				//Table of results to store the user retrieved from database matching credentials in request
				ResultSet matches = database.retrieveRecords(new String[] {"Users"},
				"Username = '" + request.getUsername() +
				"' AND Password = '" + request.getPassword() + "'", false);
				
				/*Updating server contact lists and reassigning handler's
				 *currentUser instance variable in case there are matches
				 */
				if (matches.next()) {
					//Checking if account is already online
					if (!onlineUsers.containsKey(matches.getString(1))) {
						offlineUsers.remove(matches.getString(1));
						onlineUsers.put(matches.getString(1), this);
						currentUser = matches.getString(1);
						//Logging the login
						try {
							logInformation("User, " + currentUser + ", has logged in at client, "
									+ handlerSocket.getInetAddress(), false);
						}
						catch (IOException ex) {
							System.err.println("Failed to write to log file: (" + ex + ")");
						}
						//Notifying all clients that the user has logged in
						if (!notificationsExecutor.isShutdown())
							notificationsExecutor.execute(() -> sendNotification(new ServerNotification("User '" +
							currentUser.toString() + "', is now online.")));
						return new User(currentUser); //Returning user object
					}
					else {
						//Returning an error response in case the account is already logged in on another client
						return new ServerError("Your account is already logged in on another client.");
					}
				}
				else {
					//Returning an error response in case the credentials don't match any records
					return new ServerError("The credentials you entered are invalid.");
				}
			}
			//Returning an error response in case there is failure communicating with the database
			catch (SQLException ex) {
				//Logging the exception
				try {
					logInformation("Error at handler for client " + handlerSocket.getInetAddress() +
							": " + ex, false);
				}
				catch (IOException e) {
					System.err.println("Failed to write to log file: " + e);
				}
				return new ServerError("Unable to log you in: "
						+ "There was an error communicating with the G-Instant Messenger database");
			}
		}

		//Method creating user as specified in the Authentication request credentials
		private ServerMessage createAccount(Authentication request) {
			try {
				//Checking if username is already taken
				if (!database.retrieveRecords(new String[] {"Users"}, "Username = '" +
					request.getUsername() + "'", false).next()) {
			
					//Adding user to database and onlineUsers list
					database.addRecord("Users", new String[] {request.getUsername(), request.getPassword(),
					Timestamp.valueOf(LocalDateTime.now()).toString()});
					onlineUsers.put(request.getUsername(), this);
					//Assigning user's username to handler's current username instance variable
					currentUser = request.getUsername();

					//Logging the sign up
					try {
						logInformation("User, " + currentUser + ", has just signed up and logged in at "
						+ handlerSocket.getInetAddress(), false);
					}
					catch (IOException ex) {
						System.err.println("Failed to write to log file: (" + ex + ")");
					}

					//Notifying all clients that the user has joined the network
					if (!notificationsExecutor.isShutdown())
						notificationsExecutor.execute(() -> sendNotification(new ServerNotification("User '" +
						currentUser.toString() + "', is now online")));
					
					return new User(currentUser);
				}
				//Returning an error response in case the username already exists
				else {
					return new ServerError("Unable to create your account. "
							+ "The provided username is already in use.");
				}
			}
			//Returning an error response in case there is failure communicating with the database
			catch (SQLException ex) {
				//Logging the exception
				try {
					logInformation("Error at handler for client " + handlerSocket.getInetAddress() +
							": " + ex, false);
				}
				catch (IOException e) {
					System.err.println("Failed to write to log file: " + e);
				}
				return new ServerError("Unable to create your account: "
						+ "There was an error communicating with the G-Instant Messenger database");
			}
		}
		
		//Method disconnecting the client
		private void disconnectClient() {
			//Logging user out first in case the client terminated with an account logged in
			if (currentUser != null) {
				ServerMessage userDisconnectResponse = disconnectUser(new UserDisconnection());
				//Execute in case user disconnection fails
				if(userDisconnectResponse instanceof ServerError) {
					onlineUsers.remove(currentUser);
					offlineUsers.put(currentUser, LocalDateTime.now());
					currentUser = null;
				}
			}
			try {
				//Closing the socket resource if not already (particularly for when shutdown sequence is initiated)
				if (!handlerSocket.isClosed())
					handlerSocket.close();
			}
			catch (IOException e) {}
			//Logging the disconnection
			try {
				logInformation("Client, " + handlerSocket.getInetAddress() +
				", has disconnected from server.", false);
			}
			catch (IOException ex) {
				System.err.println("Failed to write to log file: (" + ex + ")");
			}
		}
		
		//Method to send server message to client
		synchronized void sendServerMessage(ServerMessage message) throws IOException {
			outgoingServerMessages.writeObject(message);
			outgoingServerMessages.flush();
		}
		
		//Method to send notification to client(s)
		private void sendNotification(ServerMessage notification) {
			//If notification is a chat message
			if (notification instanceof ChatMessage) {
				ChatMessage chat = (ChatMessage) notification;
				//Send message if the user is online
				if (onlineUsers.containsKey(chat.getRecipient())) {
					try {
						onlineUsers.get(chat.getRecipient()).sendServerMessage(notification);
					}
					catch (IOException ex) {}
				}
			}
			//If the notifications is a broadcast server notification
			else if (notification instanceof ServerNotification) {
				onlineUsers.forEach((k, v) -> {
					if (!k.equals(currentUser)) {
						try {
							v.sendServerMessage(notification);
						} catch (IOException ex) {
						}
					}
				});
			} 
		}
	}
}
