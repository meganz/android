package mega.privacy.android.app.lollipop.tempMegaChatClasses;


import java.util.ArrayList;
import java.util.Date;

import mega.privacy.android.app.MegaContact;

public class RecentChat {

    ArrayList<ChatRoom> recentChats;

    ChatRoom chat1;
    ChatRoom chat2;
    ChatRoom chat3;
    ChatRoom chat4;
    ChatRoom chat5;

    public RecentChat(){

        recentChats = new ArrayList<ChatRoom>();

        chat1 = new ChatRoom();
        MegaContact user1 = new MegaContact();
        user1.setMail("android111@yopmail.com");
        ArrayList<MegaContact> contacts1 = new ArrayList<MegaContact>();
        contacts1.add(user1);
        chat1.setContacts(contacts1);

        ArrayList<Message> messagesList1 = new ArrayList<Message>();

        Message message1 = new Message();
        message1.setMessage("Hola");
        message1.setDate(new Date(1472037610));
        message1.setType(Message.TEXT);
        messagesList1.add(message1);

        Message message2 = new Message();
        message2.setMessage("Thank you!I'm good! How is Jess? Are you still in town?");
        message2.setDate(new Date(1472037667));
        message1.setType(Message.TEXT);
        messagesList1.add(message2);

        chat1.setMessages(messagesList1);
        recentChats.add(chat1);

    }

    public ArrayList<ChatRoom> getRecentChats() {
        return recentChats;
    }

    public void setRecentChats(ArrayList<ChatRoom> recentChats) {
        this.recentChats = recentChats;
    }


}
