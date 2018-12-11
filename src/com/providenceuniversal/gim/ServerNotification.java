package com.providenceuniversal.gim;

public class ServerNotification implements ServerMessage {
	
	private static final long serialVersionUID = 5924024651514134122L;
	private final String message;
	
	public ServerNotification(String message) {
		this.message = message;
	}
	
	String getMessage() {
		return message;
	}

	@Override
	public String toString() {
		return message;
	}
	
	
}
