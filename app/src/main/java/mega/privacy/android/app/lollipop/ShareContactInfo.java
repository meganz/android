package mega.privacy.android.app.lollipop;

import android.support.annotation.NonNull;

import mega.privacy.android.app.MegaContactAdapter;
import mega.privacy.android.app.MegaContactDB;
import nz.mega.sdk.MegaUser;

/**
 * Created by mega on 20/02/18.
 */

public class ShareContactInfo{

    long id;
    String name;
    String email;
    String phoneNumber;

    MegaContactDB megaContactDB;
    MegaUser megaUser;
    String fullName;

    boolean phoneContactInfo;
    boolean megaContactAdapter;

    public ShareContactInfo(long id, String name, String email, String phoneNumber, MegaContactDB megaContactDB, MegaUser megaUser, String fullName, boolean phoneContactInfo, boolean megaContactAdapter) {
        this.id = id;
        this.name = name;
        this.email = email;
        this.phoneNumber = phoneNumber;


        this.megaContactDB = megaContactDB;
        this.megaUser = megaUser;
        this.fullName = fullName;

        this.phoneContactInfo = phoneContactInfo;
        this.megaContactAdapter = megaContactAdapter;
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

    public MegaContactDB getMegaContactDB() {
        return megaContactDB;
    }

    public void setMegaContactDB(MegaContactDB megaContactDB) {
        this.megaContactDB = megaContactDB;
    }

    public MegaUser getMegaUser() {
        return megaUser;
    }

    public void setMegaUser(MegaUser megaUser) {
        this.megaUser = megaUser;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public boolean getPhoneContactInfo() {
        return phoneContactInfo;
    }

    public void setPhoneContactInfo(boolean phoneContactInfo) {
        this.phoneContactInfo = phoneContactInfo;
    }

    public boolean getMegaContactAdapter() {
        return megaContactAdapter;
    }

    public void setMegaContactAdapter(boolean megaContactAdapter) {
        this.megaContactAdapter = megaContactAdapter;
    }
}
