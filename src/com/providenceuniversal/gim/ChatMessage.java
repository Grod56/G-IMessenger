package com.providenceuniversal.gim;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
/**
 * The {@code ChatMessage} class represents a single chat message between a given sender
 * and recipient. It stores the message body, sender, recipient, and time stamp information
 * of the message. It is one of the types of {@code ClientMessage}.
 * 
 * @author Garikai Gumbo<br>
 * Providence Universal Studios®<br>
 * Copyright © 2018.<br>
 * All rights reserved.
 * @version 1.0
 *
 */
public class ChatMessage implements ClientMessage{
	
	private static final long serialVersionUID = 7670884855089704404L;
	private String sender;
	private String recipient;
	private String body;
	private LocalDateTime timeStamp;
	
	/**
	 * Creates {@code ChatMessage} object and sets the time stamp to {@code LocalDateTime.now()}
	 */
	public ChatMessage() {
		super();
		timeStamp = LocalDateTime.now();
	}
	
	/**
	 * Creates {@code ChatMessage} object and sets the time stamp to {@code LocalDateTime.now()},
	 * and the sender and receiver to their corresponding parameter values.
	 * 
	 * @param sender Sender
	 * @param recipient Recipient
	 * @param body Message body
	 */
	public ChatMessage(String sender, String recipient, String body) {
		this();
		this.sender = sender;
		this.recipient = recipient;
		this.body = body;
	}
	
	/**
	 * Creates {@code ChatMessage} object and sets the time stamp, sender
	 * and receiver to their corresponding parameter values.
	 * 
	 * @param sender Sender
	 * @param recipient Recipient
	 * @param body Message body
	 * @param timeStamp Message time stamp
	 */
	public ChatMessage(String sender, String recipient, String body, LocalDateTime timeStamp) {
		super();
		this.sender = sender;
		this.recipient = recipient;
		this.body = body;
		this.timeStamp = timeStamp;
	}
	
	//_________________________________Getters and setters for each field_________________________________

	public String getSender() {
		return sender;
	}
	public void setSender(String sender) {
		this.sender = sender;
	}
	public String getRecipient() {
		return recipient;
	}
	public void setRecipient(String recipient) {
		this.recipient = recipient;
	}
	public String getBody() {
		return body;
	}
	public void setBody(String body) {
		this.body = body;
	}
	public LocalDateTime getTimeStamp() {
		return timeStamp;
	}
	public void setTimeStamp(LocalDateTime timeStamp) {
		this.timeStamp = timeStamp;
	}

	//_______________________________________________________________________________________________
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((body == null) ? 0 : body.hashCode());
		result = prime * result + ((recipient == null) ? 0 : recipient.hashCode());
		result = prime * result + ((sender == null) ? 0 : sender.hashCode());
		result = prime * result + ((timeStamp == null) ? 0 : timeStamp.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		ChatMessage other = (ChatMessage) obj;
		if (body == null) {
			if (other.body != null)
				return false;
		} else if (!body.equals(other.body))
			return false;
		if (recipient == null) {
			if (other.recipient != null)
				return false;
		} else if (!recipient.equals(other.recipient))
			return false;
		if (sender == null) {
			if (other.sender != null)
				return false;
		} else if (!sender.equals(other.sender))
			return false;
		if (timeStamp == null) {
			if (other.timeStamp != null)
				return false;
		} else if (!timeStamp.equals(other.timeStamp))
			return false;
		return true;
		
		
	}

	/**
	 * Returns a {@code String} representation of the chat message in the given format: <br>
	 * [timestamp] sender > body
	 * 
	 */
	@Override
	public String toString() {
		return "[" + timeStamp.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"))
		+ "] " + sender + "> " + body;
	}
	
	
}
