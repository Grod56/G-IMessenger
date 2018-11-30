package com.providenceuniversal.gim;

/**
 * The {@code Authentication} class stores credentials to be used to login, create an account
 * or delete an account;--based on the authenticationType assigned to the particular instance.
 * It is one of the types of {@code ClientMessage}.
 * 
 * @author Garikai Gumbo<br>
 * Providence Universal Studios®<br>
 * Copyright © 2018.<br>
 * All rights reserved.
 * @version 1.0
 */
public class Authentication implements ClientMessage{

	/**
	 * Enumeration which stores, the types of Authentication
	 * 
	 * @author Garikai Gumbo<br>
	 * Providence Universal Studios®<br>
	 * Copyright © 2018.<br>
	 * All rights reserved.
	 * @version 1.0
	 *
	 */
	public static enum Type {LOGIN, ACCOUNT_CREATION, ACCOUNT_DELETION}

	private static final long serialVersionUID = 2667643613192464372L;
	private String username, password;
	private Type authenticationType;
	
	public Authentication() {}
	
	/**
	 * Creates new {@code Authentication} object and initializes credentials and authentication type with
	 * the parameter values.
	 * 
	 * @param username Username 
	 * @param password Password
	 * @param authenticationType Type of authentication request
	 */
	public Authentication(String username, String password, Type authenticationType) {
		this.username = username;
		this.password = password;
		this.authenticationType = authenticationType;
	}
	
	//_________________________________Getters and setters for each field_________________________________

	String getUsername() {
		return username;
	}
	void setUsername(String username) {
		this.username = username;
	}
	String getPassword() {
		return password;
	}
	void setPassword(String password) {
		this.password = password;
	}
	Type getAuthenticationType() {
		return authenticationType;
	}
	void setAuthenticationType(Type authenticationType) {
		this.authenticationType = authenticationType;
	}
}
