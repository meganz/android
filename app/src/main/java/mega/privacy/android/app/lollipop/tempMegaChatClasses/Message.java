package mega.privacy.android.app.lollipop.tempMegaChatClasses;


import java.util.Date;

import mega.privacy.android.app.MegaContact;

public class Message {

    public static int TEXT = 0;
    public static int VIDEO = 1;
    public static int AUDIO = 2;

    String message;
    Date date;
    MegaContact user;
    int type;
    long duration;

    public Date getDate() {
        return date;
    }

    public void setDate(Date date) {
        this.date = date;
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
}
