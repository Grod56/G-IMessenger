package com.providenceuniversal.gim;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.stream.Collectors;

/**
 * The {@code ChatHistory} class stores a list of chat messages among given participants
 * It is one of the types of {@code ServerMessage}.
 * 
 * @author Garikai Gumbo<br>
 * Providence Universal Studios®<br>
 */
public class ChatHistory implements ServerMessage{

	private static final long serialVersionUID = 5319559628706847566L;
	private ArrayList<ChatMessage> chats;
	
	/**
	 * Creates new {@code ChatHistory} object and initializes chats list with
	 * the {@code chatsQuery} values.
	 * @param chatsQuery ResultSet storing database query containing chat messages
	 * @throws SQLException
	 */
	public ChatHistory(ResultSet chatsQuery) throws SQLException {
		chats = new ArrayList<ChatMessage>();
		while(chatsQuery.next()) {
			ChatMessage chat = new ChatMessage(chatsQuery.getString(2), chatsQuery.getString(3),
					chatsQuery.getString(4), chatsQuery.getTimestamp(5).toLocalDateTime());
			chats.add(chat);
		}
	}

	//_________________________________Getters and setters for each field_________________________________
	
	public ArrayList<ChatMessage> getChats() {
		return chats;
	}

	//____________________________________________________________________________________________________

	/**
	 * Returns a {@code String} representation of the chat history in the given format: <br>
	 * Chat History:<br>
	 * {@code ChatMessage}<br>
	 * {@code ChatMessage}<br>
	 * .<br>
	 * .<br>
	 * .
	 */
	@Override
	public String toString() {
		
		return "Chat History:\n" + chats.stream().map(chat -> chat.toString()).collect(Collectors.joining("\n"));
	}
	
	
	
}
