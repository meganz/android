package mega.privacy.android.app.lollipop;

import android.graphics.Bitmap;
import android.os.Parcel;
import android.os.Parcelable;
import android.text.TextUtils;

import java.util.List;
import java.util.Objects;

public class InvitationContactInfo implements Parcelable, Cloneable {

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
    private static final String AT_SIGN = "@";
    private long id;
    private boolean isHighlighted;
    private int type;
    private Bitmap bitmap;
    private String name, displayInfo, handle;
    private int avatarColor;

    /**
     * Phone numbers and emails which don't exist on MEGA.
     */
    private List<String> filteredContactInfos;

    public InvitationContactInfo(long id, String name, int type, List<String> filteredContactInfos, String displayInfo, int avatarColor) {
        this.id = id;
        this.type = type;
        this.filteredContactInfos = filteredContactInfos;
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

    public void setDisplayInfo(String displayInfo) {
        this.displayInfo = displayInfo;
    }

    public static InvitationContactInfo createManualInputEmail(String inputString, int avatarColor) {
        return new InvitationContactInfo(inputString.hashCode(), "", TYPE_MANUAL_INPUT_EMAIL, null, inputString, avatarColor);
    }

    public static InvitationContactInfo createManualInputPhone(String inputString, int avatarColor) {
        return new InvitationContactInfo(inputString.hashCode(), "", TYPE_MANUAL_INPUT_PHONE, null, inputString, avatarColor);
    }

    public List<String> getFilteredContactInfos() {
        return filteredContactInfos;
    }

    public boolean hasMultipleContactInfos() {
        if(filteredContactInfos == null) {
            return false;
        }
        return filteredContactInfos.size() > 1;
    }

    @Override
    public String toString() {
        return "\n{" +
                "id=" + id +
                ", isHighlighted=" + isHighlighted +
                ", type=" + type +
                ", bitmap=" + bitmap +
                ", name='" + name + '\'' +
                ", displayInfo='" + displayInfo + '\'' +
                ", handle='" + handle + '\'' +
                ", avatarColor='" + avatarColor + '\'' +
                '}';
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

    public String getEmail() {
        return getDisplayInfo();
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

    public int getAvatarColor() {
        return avatarColor;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeLong(id);
        dest.writeInt(isHighlighted ? 1 : 0);
        dest.writeInt(type);
        dest.writeString(name);
        dest.writeString(displayInfo);
        dest.writeString(handle);
        dest.writeInt(avatarColor);
    }

    public InvitationContactInfo(Parcel in) {
        id = in.readLong();
        isHighlighted = in.readInt() == 1;
        type = in.readInt();
        name = in.readString();
        displayInfo = in.readString();
        handle = in.readString();
        avatarColor = in.readInt();
    }

    @Override
    public Object clone() throws CloneNotSupportedException {
        return super.clone();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        InvitationContactInfo info = (InvitationContactInfo) o;
        return id == info.id &&
                displayInfo.equals(info.displayInfo);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, displayInfo);
    }
}
