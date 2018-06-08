package mega.privacy.android.app;

import android.os.Parcel;
import android.os.Parcelable;

public class MegaContactDB implements Parcelable{
	String handle;
	String name;
	String lastName;
	String mail;
	
	public MegaContactDB(String handle, String mail, String name, String lastName) {
		super();
		this.handle = handle;
		this.name = name;
		this.lastName = lastName;
		this.mail = mail;
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

	@Override
	public int describeContents() {
		return 0;
	}

	@Override
	public void writeToParcel(Parcel dest, int flags) {
		dest.writeString(handle);
		dest.writeString(name);
		dest.writeString(lastName);
		dest.writeString(mail);
	}

	protected MegaContactDB(Parcel in) {
		handle = in.readString();
		name = in.readString();
		lastName = in.readString();
		mail = in.readString();
	}

	public static final Creator<MegaContactDB> CREATOR = new Creator<MegaContactDB>() {
		@Override
		public MegaContactDB createFromParcel(Parcel in) {
			return new MegaContactDB(in);
		}

		@Override
		public MegaContactDB[] newArray(int size) {
			return new MegaContactDB[size];
		}
	};

}
