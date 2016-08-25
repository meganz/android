package mega.privacy.android.app.lollipop.tempMegaChatClasses;

import java.util.ArrayList;

import mega.privacy.android.app.MegaContact;

public class ChatRoom {

    ArrayList<Message> messages;
    ArrayList<MegaContact> contacts;

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
}
