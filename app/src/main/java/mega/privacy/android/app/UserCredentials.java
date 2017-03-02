package mega.privacy.android.app;

public class UserCredentials {
	private String email;
	private String session;
	private String firstName;
	private String lastName;
	private String myHandle;

	public UserCredentials(String email){
		this.email = email;
	}
	
	public UserCredentials(String email, String session, String firstName, String lastName, String myHandle) {
		this.email = email;
		this.session = session;
		this.firstName = firstName;
		this.lastName = lastName;
		this.myHandle = myHandle;
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

	public String getMyHandle() {
		return myHandle;
	}

	public void setMyHandle(String myHandle) {
		this.myHandle = myHandle;
	}
}

