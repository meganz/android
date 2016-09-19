package mega.privacy.android.app.lollipop.tempMegaChatClasses;

import java.util.ArrayList;

import mega.privacy.android.app.MegaContact;

public class ChatRoom {

    long id;
    ArrayList<Message> messages;
    ArrayList<MegaContact> contacts;
    int unreadMessages;

    public boolean isMute() {
        return isMute;
    }

    public void setMute(boolean mute) {
        isMute = mute;
    }

    boolean isMute;

    public ArrayList<MegaContact> getContacts() {
        return contacts;
    }

    public void setContacts(ArrayList<MegaContact> contacts) {
        this.contacts = contacts;
    }

    public ArrayList<Message> getMessages() {
        return messages;
    }

    public void setMessages(ArrayList<Message> messages) {
        this.messages = messages;
    }

    public int getUnreadMessages() {
        return unreadMessages;
    }

    public void setUnreadMessages(int unreadMessages) {
        this.unreadMessages = unreadMessages;
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }
}
