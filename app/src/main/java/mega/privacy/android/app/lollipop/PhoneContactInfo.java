package mega.privacy.android.app.lollipop;

import android.support.annotation.NonNull;

public class PhoneContactInfo implements  Comparable<PhoneContactInfo>{
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
}
