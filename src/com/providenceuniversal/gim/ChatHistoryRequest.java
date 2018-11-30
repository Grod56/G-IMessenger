package com.providenceuniversal.gim;
/**
 * The {@code ChatHistoryRequest} class stores a list of participants to be used 
 * to retrieve the chat history between the participants.
 * It is one of the types of {@code ClientMessage}.
 * 
 * @author Garikai Gumbo<br>
 * Providence Universal Studios�<br>
 * Copyright � 2018.<br>
 * All rights reserved.
 * @version 1.0
 */
public class ChatHistoryRequest implements ClientMessage{

	private static final long serialVersionUID = 4017620732994082282L;
	private String participant1;
	private String participant2;
	
	/**
	 * Creates new {@code ChatHistoryRequest} object and initializes the participants list with
	 * the parameter values
	 * 
	 * @param participant1 One of the participants
	 * @param participant2 The other of the participants
	 */
	public ChatHistoryRequest(String participant1, String participant2) {
		super();
		this.participant1 = participant1;
		this.participant2 = participant2;
	}

	//_________________________________Getters and setters for each field_________________________________
	
	public String getParticipant1() {
		return participant1;
	}

	public void setParticipant1(String participant1) {
		this.participant1 = participant1;
	}

	public String getParticipant2() {
		return participant2;
	}

	public void setParticipant2(String participant2) {
		this.participant2 = participant2;
	}
	
	
	
	
	
}