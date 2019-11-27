package mega.privacy.android.app.lollipop;

import mega.privacy.android.app.MegaContactAdapter;

/**
 * Created by mega on 20/02/18.
 */

public class ShareContactInfo{

    PhoneContactInfo phoneContactInfo;
    MegaContactAdapter megaContactAdapter;
    String mail;
    boolean isPhoneContact;
    boolean isMegaContact;
    boolean isHeader;
    boolean isProgress;

    public ShareContactInfo(PhoneContactInfo phoneContactInfo, MegaContactAdapter megaContactAdapter, String mail) {
        this.phoneContactInfo = phoneContactInfo;
        if (phoneContactInfo != null) {
            isPhoneContact = true;
        }
        else {
            isPhoneContact = false;
        }

        this.megaContactAdapter = megaContactAdapter;
        if (megaContactAdapter != null) {
            isMegaContact = true;
        }
        else {
            isMegaContact = false;
        }

        this.mail = mail;
        isHeader = false;
        isProgress = false;
    }

    public ShareContactInfo(boolean isHeader, boolean isMegaContact, boolean isPhoneContact){
        this.isHeader = isHeader;
        this.isMegaContact = isMegaContact;
        this.isPhoneContact = isPhoneContact;
        isProgress = false;
    }

    public ShareContactInfo() {
        isPhoneContact = false;
        isMegaContact = false;
        isHeader = false;
        isProgress = true;
    }

    public PhoneContactInfo getPhoneContactInfo() {
        return phoneContactInfo;
    }

    public void setPhoneContactInfo(PhoneContactInfo phoneContactInfo) {
        this.phoneContactInfo = phoneContactInfo;
    }

    public MegaContactAdapter getMegaContactAdapter() {
        return megaContactAdapter;
    }

    public void setMegaContactAdapter(MegaContactAdapter megaContactAdapter) {
        this.megaContactAdapter = megaContactAdapter;
    }

    public String getMail () {
        return mail;
    }

    public boolean isHeader () {
        return isHeader;
    }

    public boolean isPhoneContact () {
        return isPhoneContact;
    }

    public boolean isMegaContact () {
        return isMegaContact;
    }

    public boolean isProgress() {
        return isProgress;
    }
}
