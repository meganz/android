package mega.privacy.android.app.lollipop;

import android.os.Parcel;
import android.os.Parcelable;

public class ContactInfo extends PhoneContactInfo{

    public static final int TYPE_MEGA_CONTACT_HEADER = 0;
    public static final int TYPE_PHONE_CONTACT_HEADER = 1;
    public static final int TYPE_MEGA_CONTACT = 2;
    public static final int TYPE_PHONE_CONTACT = 3;

    private int type;

    public ContactInfo(long id, String name, String email, String phoneNumber, int type) {
        super(id, name,email,phoneNumber);
        this.type = type;
    }

    public int getType(){
        return type;
    }
}
