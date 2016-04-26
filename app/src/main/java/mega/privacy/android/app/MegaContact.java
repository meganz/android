package mega.privacy.android.app;

import nz.mega.sdk.MegaUser;

public class MegaContact {
	String handle;
	String name;
	String lastName;
	String mail;
	
	public MegaContact(String handle, String mail, String name, String lastName) {
		super();
		this.handle = handle;
		this.name = name;
		this.lastName = lastName;
		this.mail = mail;
	}

	public String getHandle() {
		return handle;
	}
	public void setHandle(String handle) {
		this.handle = handle;
	}
	public String getName() {
		return name;
	}
	public void setName(String name) {
		this.name = name;
	}
	public String getMail() {
		return mail;
	}
	public void setMail(String mail) {
		this.mail = mail;
	}

	public String getLastName() {
		return lastName;
	}

	public void setLastName(String lastName) {
		this.lastName = lastName;
	}	
}
