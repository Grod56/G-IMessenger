package com.providenceuniversal.gim;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * The {@code ContactList} class stores a list of all the users of G-Instant 
 * Messenger, and indicates their activity status. 
 * It is one of the types of {@code ServerMessage}.
 * 
 * @author Garikai Gumbo<br>
 * Providence Universal Studios®<br>
 */
public class ContactList implements ServerMessage{

	private static final long serialVersionUID = -8209371024479092657L;
	private HashMap<String, String> contactsMap;
	private String[] contactNames;
	
	/**
	 * Creates {@code ContactList} object and fills the contact list with parameter value
	 * @param contactsMap HashMap containing the contacts
	 */
	public ContactList(HashMap<String, String> contactsMap) {
		this.contactsMap = contactsMap;
		contactNames = contactsMap.keySet().toArray(new String[] {});
	}
	
	//_________________________________Getters and setters for each field_________________________________

	public HashMap<String, String> getContactsMap() {
		return contactsMap;
	}

	void setContactsMap(HashMap<String, String> contactsMap) {
		this.contactsMap = contactsMap;
		contactNames = contactsMap.keySet().toArray(new String[] {});
	}
	
	public String getContactName(int position) {
		return contactNames[position - 1];
	}
	
	//____________________________________________________________________________________________________
	
	void removeContact(String contactName) {
		contactsMap.remove(contactName);
		contactNames = contactsMap.keySet().toArray(new String[] {});
	}

	int getTotalNumberOfContacts() {
		return contactsMap.size();	
	}
	
	@Override
	/**
	 * Returns a {@code String} representation of the contact list in the given format: <br>
	 * Contacts on G-Instant Messenger:<br>
	 * [Contact name]:		[Activity Status]
	 */
	public String toString() {
		return "Contacts on G-Instant Messenger:\n" + IntStream.range(0, contactNames.length)
				.mapToObj(index -> index + 1 + ". " + contactNames[index] +
						":\t" + contactsMap.get(contactNames[index]))
				.collect(Collectors.joining("\n"));
	}
	
	
	
}
