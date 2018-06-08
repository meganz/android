package mega.privacy.android.app.lollipop;

import android.os.Parcel;
import android.os.Parcelable;
import android.support.annotation.NonNull;

public class PhoneContactInfo implements  Comparable<PhoneContactInfo>, Parcelable{
    long id;
    String name;
    String email;
    String phoneNumber;

    public PhoneContactInfo(long id, String name, String email, String phoneNumber) {
        this.id = id;
        this.name = name;
        this.email = email;
        this.phoneNumber = phoneNumber;
    }

    public long getId(){
        return id;
    }

    public String getName(){
        return name;
    }

    public String getEmail(){
        return email;
    }

    public String getPhoneNumber(){
        return phoneNumber;
    }

    @Override
    public int compareTo(PhoneContactInfo contactInfo) {

        String a = new String(String.valueOf(this.getName()));
        String b = new String (String.valueOf(contactInfo.getName()));

        return a.compareTo(b);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeLong(id);
        dest.writeString(name);
        dest.writeString(email);
        dest.writeString(phoneNumber);
    }

    protected PhoneContactInfo(Parcel in) {
        id = in.readLong();
        name = in.readString();
        email = in.readString();
        phoneNumber = in.readString();
    }

    public static final Creator<PhoneContactInfo> CREATOR = new Creator<PhoneContactInfo>() {
        @Override
        public PhoneContactInfo createFromParcel(Parcel in) {
            return new PhoneContactInfo(in);
        }

        @Override
        public PhoneContactInfo[] newArray(int size) {
            return new PhoneContactInfo[size];
        }
    };
}
