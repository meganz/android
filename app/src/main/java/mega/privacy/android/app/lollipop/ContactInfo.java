package mega.privacy.android.app.lollipop;

import android.graphics.Bitmap;

public class ContactInfo extends PhoneContactInfo {

    public static final int TYPE_MEGA_CONTACT_HEADER = 0;
    public static final int TYPE_PHONE_CONTACT_HEADER = 1;
    public static final int TYPE_MEGA_CONTACT = 2;
    public static final int TYPE_PHONE_CONTACT = 3;
    public static final int TYPE_MANUAL_INPUT_EMAIL = 4;
    public static final int TYPE_MANUAL_INPUT_PHONE = 5;

    private boolean isHighlighted;
    private int type;
    private Bitmap bitmap;

    public ContactInfo(long id, String name, String email, String phoneNumber, int type) {
        super(id, name, email, phoneNumber);
        this.type = type;
    }

    public int getType() {
        return type;
    }

    public void setType(int type) {
        this.type = type;
    }

    public void setBitmap(Bitmap bitmap) {
        this.bitmap = bitmap;
    }

    public Bitmap getBitmap() {
        return bitmap;
    }

    public boolean isHighlighted() {
        return isHighlighted;
    }

    public void setHighlighted(boolean highlighted) {
        isHighlighted = highlighted;
    }
}
