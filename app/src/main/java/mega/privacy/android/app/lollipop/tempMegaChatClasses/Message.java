package mega.privacy.android.app.lollipop.tempMegaChatClasses;


import mega.privacy.android.app.MegaContact;

public class Message {

    public static int TEXT = 0;
    public static int VIDEO = 1;
    public static int AUDIO = 2;

    String message;
    long dateTimestamp;
    MegaContact user;
    int type;
    long duration;
    boolean read;

    public long getDate() {
        return dateTimestamp;
    }

    public void setDate(long dateTimestamp) {
        this.dateTimestamp = dateTimestamp;
    }

    public int getType() {
        return type;
    }

    public void setType(int type) {
        this.type = type;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public MegaContact getUser() {
        return user;
    }

    public void setUser(MegaContact user) {
        this.user = user;
    }

    public long getDuration() {
        return duration;
    }

    public void setDuration(long duration) {
        this.duration = duration;
    }

    public boolean isRead() {
        return read;
    }

    public void setRead(boolean read) {
        this.read = read;
    }

}
