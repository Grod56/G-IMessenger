package com.providenceuniversal.gim;

import java.time.LocalDateTime;

/**
 * The {@code UserDisconnection} class represents a user logout request
 * from the server and is used as such. It stores the disconnection time.
 * It is one of the types of {@code ClientMessage}.
 * 
 * @author Garikai Gumbo<br>
 * Providence Universal Studios®<br>
 */
public class UserDisconnection implements ClientMessage{

	private static final long serialVersionUID = -523437620577113206L;
	private final LocalDateTime disconnectionTime;
	
	/**
	 * Creates {@code UserDisconnection} object and assigns {@code LocalDateTime.now()}
	 * to the disconnection time.
	 */
	public UserDisconnection() {
		disconnectionTime = LocalDateTime.now();
	}
	
	//_________________________________Getters and setters for each field_________________________________

	public LocalDateTime getDisconnectionTime() {
		return disconnectionTime;
	}
	
}