package mega.privacy.android.app.components.tokenautocomplete;

import java.io.Serializable;

public class ContactInfo implements Serializable{
    private String name;
    private String email;

    public ContactInfo(){
        this.name = "";
        this.email = "";
    }

    public ContactInfo(String name, String email) {
        this.name = name;
        this.email = email;
    }

    public String getName() {
        return name;
    }

    public String getEmail() {
        return email;
    }

    public void setName(String name){
        this.name = name;
    }

    public void setEmail(String email){
        this.email = email;
    }

    @Override
    public String toString(){
        return name + " ( " + email + " )";
    }
}
