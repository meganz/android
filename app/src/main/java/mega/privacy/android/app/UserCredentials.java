package mega.privacy.android.app;

public class UserCredentials {
	private String email;
	private String session;
	
	public UserCredentials(String email){
		this.email = email;
	}
	
	public UserCredentials(String email, String session) {
		this.email = email;
		this.session = session;
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
}

