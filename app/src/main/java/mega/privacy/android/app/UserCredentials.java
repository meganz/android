package mega.privacy.android.app;

public class UserCredentials {
	private String email;
	private String session;
	private String firstName;
	private String lastName;
	
	public UserCredentials(String email){
		this.email = email;
	}
	
	public UserCredentials(String email, String session, String firstName, String lastName) {
		this.email = email;
		this.session = session;
		this.firstName = firstName;
		this.lastName = lastName;
	}
	
	public String getEmail() {
		return email;
	}
	
	public void setEmail(String email){
		this.email = email;
	}
	
	public String getSession() {
		return session;
	}
	
	public void setSession(String session){
		this.session = session;
	}

	public String getFirstName() {
		return firstName;
	}

	public void setFirstName(String firstName) {
		this.firstName = firstName;
	}

	public String getLastName() {
		return lastName;
	}

	public void setLastName(String lastName) {
		this.lastName = lastName;
	}
}

