package mega.privacy.android.app;

public class MegaContactDB {
	String handle;
	String name;
	String lastName;
	String nickname;
	String mail;

	public MegaContactDB(String handle, String mail, String name, String lastName, String nickname) {
		super();
		this.handle = handle;
		this.name = name;
		this.lastName = lastName;
		this.mail = mail;
		this.nickname = nickname;
	}

	public MegaContactDB() {
		super();
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

	public String getNickname() {
		return nickname;
	}

	public void setNickname(String nickname) {
		if (nickname != null && nickname.trim().length() <= 0) {
			this.nickname = null;
		} else {
			this.nickname = nickname;
		}
	}

}
