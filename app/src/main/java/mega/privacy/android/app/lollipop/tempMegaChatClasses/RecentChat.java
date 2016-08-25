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
        MegaContact ownerMessage1 = new MegaContact();
        ownerMessage1.setMail("android555@yopmail.com");
        message1.setUser(ownerMessage1);
        messagesList1.add(message1);

        Message message2 = new Message();
        message2.setMessage("Thank you!I'm good! How is Jess? Are you still in town?");
        message2.setDate(new Date(1472037667));
        message2.setType(Message.TEXT);
        MegaContact ownerMessage2 = new MegaContact();
        ownerMessage2.setMail("android111@yopmail.com");
        message2.setUser(ownerMessage2);
        messagesList1.add(message2);

        chat1.setMessages(messagesList1);
        recentChats.add(chat1);

        chat2 = new ChatRoom();
        MegaContact user2 = new MegaContact();
        user2.setMail("android222@yopmail.com");
        ArrayList<MegaContact> contacts2 = new ArrayList<MegaContact>();
        contacts2.add(user2);
        chat2.setContacts(contacts2);

        ArrayList<Message> messagesList2 = new ArrayList<Message>();

        Message message3 = new Message();
        message3.setMessage("Hola");
        message3.setDate(new Date(1472049108));
        message3.setType(Message.TEXT);
        MegaContact ownerMessage3 = new MegaContact();
        ownerMessage3.setMail("android222@yopmail.com");
        message3.setUser(ownerMessage3);
        messagesList2.add(message3);

        Message message4 = new Message();
        message4.setMessage("Have a great night!");
        message4.setDate(new Date(1472049198));
        message4.setType(Message.TEXT);
        MegaContact ownerMessage4 = new MegaContact();
        ownerMessage4.setMail("android555@yopmail.com");
        message4.setUser(ownerMessage4);
        messagesList2.add(message4);

        chat2.setMessages(messagesList2);
        recentChats.add(chat2);

    }

    public ArrayList<ChatRoom> getRecentChats() {
        return recentChats;
    }

    public void setRecentChats(ArrayList<ChatRoom> recentChats) {
        this.recentChats = recentChats;
    }


}
