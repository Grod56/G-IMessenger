package com.providenceuniversal.gim;

/**
 * The {@code ServerError} class represents a message notifying the client 
 * that an error has occurred processing a request.
 * It is one of the types of {@code ServerMessage}.
 * 
 * @author Garikai Gumbo<br>
 * Providence Universal Studios®<br>
 */
public class ServerError implements ServerMessage {
	
	private static final long serialVersionUID = -8401486924648823744L;
	private String errorMessage;
	
	/**
	 * Creates {@code ServerError} object and initializes the error message
	 * details with the parameter values
	 * @param errorMessage Error message details
	 */
	ServerError(String errorMessage) {
		this.errorMessage = errorMessage;
	}
	
	//_________________________________Getters and setters for each field_________________________________

	public String getErrorMessage() {
		return errorMessage;
	}

	public void setErrorMessage(String errorMessage) {
		this.errorMessage = errorMessage;
	}

	//____________________________________________________________________________________________________
	
	/**
	 * Returns the message details of the {@code ServerError}
	 */
	@Override
	public String toString() {
		return errorMessage;
	}
	
	

	

}
