package com.providenceuniversal.gim;

/**
 * The {@code CommitMessage} class represents a message notifying the client 
 * that the non-query requests to the server have been successful.
 * It is one of the types of {@code ServerMessage}.
 * 
 * @author Garikai Gumbo<br>
 * Providence Universal Studios�<br>
 */
public class CommitMessage implements ServerMessage {
	
	private static final long serialVersionUID = -4505251784940529145L;
	private final String message;
	
	/**
	 * Creates new {@code CommitMessage} and initializes the message with the
	 * parameter value.
	 * @param message Commit message
	 */
	public CommitMessage(String message) {
		this.message = message;
	}
	
	//_________________________________Getters and setters for each field_________________________________

	String getMessage() {
		return message;
	}

	
	
	//____________________________________________________________________________________________________

	/**
	 * Returns the message details of the {@code CommitMessage}
	 */
	@Override
	public String toString() {
		return message;
	}
	
	
	
}
