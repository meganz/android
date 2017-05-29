package mega.privacy.android.app;

public class EphemeralCredentials {
	private String email;
	private String password;
	private String session;
	private String firstName;
	private String lastName;

	public EphemeralCredentials(String email){
		this.email = email;
	}

	public EphemeralCredentials(String email, String password, String session, String firstName, String lastName) {
		this.email = email;
		this.password = password;
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

	public String getPassword() {
		return password;
	}

	public void setMyHandle(String password) {
		this.password= password;
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

