package mega.privacy.android.app.lollipop;

public class PhoneContactInfo {
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
}
