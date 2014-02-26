package com.mega.android;

public class UserCredentials {
	private String email;
	private String publicKey;
	private String privateKey;
	
	public UserCredentials(String email, String privateKey, String publicKey) {
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

