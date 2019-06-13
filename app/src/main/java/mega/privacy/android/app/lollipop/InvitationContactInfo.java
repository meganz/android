package mega.privacy.android.app.lollipop;

import android.graphics.Bitmap;
import android.os.Parcel;
import android.os.Parcelable;
import android.text.TextUtils;

public class InvitationContactInfo implements Parcelable {

    public static final int TYPE_MEGA_CONTACT_HEADER = 0;
    public static final int TYPE_PHONE_CONTACT_HEADER = 1;
    public static final int TYPE_MEGA_CONTACT = 2;
    public static final int TYPE_PHONE_CONTACT = 3;
    public static final int TYPE_MANUAL_INPUT_EMAIL = 4;
    public static final int TYPE_MANUAL_INPUT_PHONE = 5;
    public static final Creator<InvitationContactInfo> CREATOR = new Creator<InvitationContactInfo>() {
        @Override
        public InvitationContactInfo createFromParcel(Parcel in) {
            return new InvitationContactInfo(in);
        }

        @Override
        public InvitationContactInfo[] newArray(int size) {
            return new InvitationContactInfo[size];
        }
    };
    private final String AT_SIGN = "@";
    private long id;
    private boolean isHighlighted;
    private int type;
    private Bitmap bitmap;
    private String name, displayInfo, handle, avatarColor;

    public InvitationContactInfo(long id, String name, int type, String displayInfo, String avatarColor) {
        this.id = id;
        this.type = type;
        this.name = name;
        this.displayInfo = displayInfo;
        this.avatarColor = avatarColor;
    }

    //this constructor is only for list header
    public InvitationContactInfo(long id, String name, int type) {
        this.id = id;
        this.type = type;
        this.name = name;
        this.displayInfo = "";
    }

    public InvitationContactInfo(Parcel in) {
    }

    public int getType() {
        return type;
    }

    public void setType(int type) {
        this.type = type;
    }

    public Bitmap getBitmap() {
        return bitmap;
    }

    public void setBitmap(Bitmap bitmap) {
        this.bitmap = bitmap;
    }

    public boolean isHighlighted() {
        return isHighlighted;
    }

    public void setHighlighted(boolean highlighted) {
        isHighlighted = highlighted;
    }

    public String getName() {
        if (TextUtils.isEmpty(name)) {
            return getDisplayInfo();
        }
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getDisplayInfo() {
        if (!TextUtils.isEmpty(displayInfo)) {
            return displayInfo;
        }
        return "";
    }

    public String getHandle() {
        return handle;
    }

    public void setHandle(String handle) {
        this.handle = handle;
    }

    public boolean isEmailContact() {
        return getDisplayInfo().contains(AT_SIGN);
    }

    public String getInitial() {
        return String.valueOf(getName().charAt(0));
    }

    public String getAvatarColor() {
        return avatarColor;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {

    }
}
