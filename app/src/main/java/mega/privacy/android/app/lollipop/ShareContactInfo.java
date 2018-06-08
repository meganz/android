package mega.privacy.android.app.lollipop;

import android.os.Parcel;
import android.os.Parcelable;
import android.support.annotation.NonNull;

import mega.privacy.android.app.MegaContactAdapter;
import mega.privacy.android.app.MegaContactDB;
import nz.mega.sdk.MegaUser;

/**
 * Created by mega on 20/02/18.
 */

public class ShareContactInfo implements Parcelable{

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

        dest.writeParcelable(megaContactDB, flags);
        dest.writeString(fullName);
        dest.writeByte((byte) (phoneContactInfo ? 1 : 0));
        dest.writeByte((byte) (megaContactAdapter ? 1 : 0));
    }


    protected ShareContactInfo(Parcel in) {
        id = in.readLong();
        name = in.readString();
        email = in.readString();
        phoneNumber = in.readString();

        fullName = in.readString();
        phoneContactInfo = in.readByte() != 0;
        megaContactAdapter = in.readByte() != 0;
    }

    public static final Creator<ShareContactInfo> CREATOR = new Creator<ShareContactInfo>() {
        @Override
        public ShareContactInfo createFromParcel(Parcel in) {
            return new ShareContactInfo(in);
        }

        @Override
        public ShareContactInfo[] newArray(int size) {
            return new ShareContactInfo[size];
        }
    };
}
