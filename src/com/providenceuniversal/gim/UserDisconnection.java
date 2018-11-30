package com.providenceuniversal.gim;

import java.time.LocalDateTime;

/**
 * The {@code UserDisconnection} class represents a user logout request
 * from the server and is used as such. It stores the disconnection time.
 * It is one of the types of {@code ClientMessage}.
 * 
 * @author Garikai Gumbo<br>
 * Providence Universal Studios�<br>
 * Copyright � 2018.<br>
 * All rights reserved.
 * @version 1.0
 *
 */
public class UserDisconnection implements ClientMessage{

	private static final long serialVersionUID = -523437620577113206L;
	private LocalDateTime disconnectionTime;
	
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
	public void setDisconnectionTime(LocalDateTime disconnectionTime) {
		this.disconnectionTime = disconnectionTime;
	}
	
}