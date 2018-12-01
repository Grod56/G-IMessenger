package com.providenceuniversal.gim;

/**
 * The {@code User} represents a user of G-Instant Messenger™ and it stores the user details of the user.
 * It is immutable and it is one of types of both {@code ClientMessage} and {@code ServerMessage}.
 * 
 * @author Garikai Gumbo<br>
 * Providence Universal Studios®<br>
 */
public class User implements ClientMessage, ServerMessage{

	private static final long serialVersionUID = -8067030963168403709L;
	private final String username;
	
	/**
	 * Creates {@code User} object with the parameter value as the username for the
	 * object
	 * @param username Username
	 */
	public User(String username) {
		this.username = username;
	}
	
	//Getter for username
	public String getUsername() {
		return username;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((username == null) ? 0 : username.hashCode());
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
		User other = (User) obj;
		if (username == null) {
			if (other.username != null)
				return false;
		} else if (!username.equals(other.username))
			return false;
		return true;
	}
	
	
}
