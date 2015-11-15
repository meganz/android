package mega.privacy.android.app;

/*
 * Class to hold User credentials
 */
public class OldUserCredentials {
	private String email;
	private String publicKey;
	private String privateKey;
	
	public OldUserCredentials(String email, String privateKey, String publicKey) {
		this.email = email;
		this.privateKey = privateKey;
		this.publicKey = publicKey;
	}
	
	public String getEmail() {
		return email;
	}
	
	public String getPrivateKey() {
		return privateKey;
	}
	
	public String getPublicKey() {
		return publicKey;
	}
}