package mega.privacy.android.app.lollipop.megaachievements;


import nz.mega.sdk.MegaStringList;

public class ReferralBonus {

    long storage;
    long transfer;
    long daysLeft;
    MegaStringList emails;

    public long getStorage() {
        return storage;
    }

    public void setStorage(long storage) {
        this.storage = storage;
    }

    public long getTransfer() {
        return transfer;
    }

    public void setTransfer(long transfer) {
        this.transfer = transfer;
    }

    public long getDaysLeft() {
        return daysLeft;
    }

    public void setDaysLeft(long daysleft) {
        this.daysLeft = daysleft;
    }

    public MegaStringList getEmails() {
        return emails;
    }

    public void setEmails(MegaStringList emails) {
        this.emails = emails;
    }
}
