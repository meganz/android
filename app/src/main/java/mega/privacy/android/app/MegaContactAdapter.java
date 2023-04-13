package mega.privacy.android.app;


import mega.privacy.android.domain.entity.Contact;
import nz.mega.sdk.MegaUser;

public class MegaContactAdapter {

    Contact contact;
    MegaUser megaUser;
    String fullName;
    String lastGreen;
    private boolean isSelected;

    public MegaContactAdapter(Contact contact, MegaUser megaUser, String fullName) {
        this.contact = contact;
        this.megaUser = megaUser;
        this.fullName = fullName;
        this.lastGreen = "";
        this.isSelected = false;
    }

    public Contact getMegaContactDB() {
        return contact;
    }

    public void setMegaContactDB(Contact contact) {
        this.contact = contact;
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

    public String getLastGreen() {
        return lastGreen;
    }

    public void setLastGreen(String lastGreen) {
        this.lastGreen = lastGreen;
    }

    public boolean isSelected() {
        return isSelected;
    }

    public void setSelected(boolean isSelected) {
        this.isSelected = isSelected;
    }
}
