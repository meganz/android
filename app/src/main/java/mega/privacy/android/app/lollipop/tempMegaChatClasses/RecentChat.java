package mega.privacy.android.app.lollipop.tempMegaChatClasses;


import java.util.ArrayList;

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

        //Chat one
        chat1 = new ChatRoom();
        MegaContact user1 = new MegaContact();
        user1.setMail("android111@yopmail.com");
        user1.setHandle("-8833695690414824345");
        ArrayList<MegaContact> contacts1 = new ArrayList<MegaContact>();
        contacts1.add(user1);
        chat1.setContacts(contacts1);

        ArrayList<Message> messagesList1 = new ArrayList<Message>();

        Message message1 = new Message();
        message1.setMessage("Hola");
        message1.setDate(1472123610);
        message1.setType(Message.TEXT);
        MegaContact ownerMessage1 = new MegaContact();
        ownerMessage1.setMail("android555@yopmail.com");
        message1.setUser(ownerMessage1);
        messagesList1.add(message1);

        Message message2 = new Message();
        message2.setMessage("Thank you!I'm good! How is Jess? Ar you still in town?");
        message2.setDate(1472123750);
        message2.setType(Message.TEXT);
        message2.setRead(false);
        MegaContact ownerMessage2 = new MegaContact();
        ownerMessage2.setMail("android111@yopmail.com");
        message2.setUser(ownerMessage2);
        messagesList1.add(message2);

        chat1.setMessages(messagesList1);
        chat1.setUnreadMessages(3);
        recentChats.add(chat1);

        //Chat 2
        chat2 = new ChatRoom();
        MegaContact user2 = new MegaContact();
        user2.setMail("android222@yopmail.com");
        user2.setHandle("1883490498849024631");
        ArrayList<MegaContact> contacts2 = new ArrayList<MegaContact>();
        contacts2.add(user2);
        chat2.setContacts(contacts2);

        ArrayList<Message> messagesList2 = new ArrayList<Message>();

        Message message3 = new Message();
        message3.setMessage("Hola");
        message3.setDate(1471724060);
        message3.setType(Message.TEXT);
        MegaContact ownerMessage3 = new MegaContact();
        ownerMessage3.setMail("android222@yopmail.com");
        message3.setUser(ownerMessage3);
        messagesList2.add(message3);

        Message message4 = new Message();
        message4.setMessage("Have a great night!");
        message4.setDate(1471724150);
        message4.setType(Message.TEXT);
        message4.setRead(true);
        MegaContact ownerMessage4 = new MegaContact();
        ownerMessage4.setMail("android555@yopmail.com");
        message4.setUser(ownerMessage4);
        messagesList2.add(message4);

        chat2.setMessages(messagesList2);
        chat2.setUnreadMessages(0);
        recentChats.add(chat2);

        //Chat 3
        chat3 = new ChatRoom();
        MegaContact user3 = new MegaContact();
        user3.setMail("android333@yopmail.com");
        user3.setHandle("-4662504005972119692");
        ArrayList<MegaContact> contacts3 = new ArrayList<MegaContact>();
        contacts3.add(user3);
        chat3.setContacts(contacts3);

        ArrayList<Message> messagesList3 = new ArrayList<Message>();

        Message message5 = new Message();
        message5.setMessage("Call me!");
        message5.setDate(1470074400);
        message5.setType(Message.TEXT);
        MegaContact ownerMessage5 = new MegaContact();
        ownerMessage5.setMail("android555@yopmail.com");
        message5.setUser(ownerMessage5);
        messagesList3.add(message5);

        Message message6 = new Message();
        message6.setMessage("Have a great night!");
        message6.setDate(1470074450);
        message6.setType(Message.VIDEO);
        message6.setDuration(210);
        MegaContact ownerMessage6 = new MegaContact();
        ownerMessage6.setMail("android333@yopmail.com");
        message6.setUser(ownerMessage4);
        messagesList3.add(message6);

        chat3.setMessages(messagesList3);
        chat3.setUnreadMessages(0);
        recentChats.add(chat3);

        //Chat 4
        chat4 = new ChatRoom();
        MegaContact user4 = new MegaContact();
        user4.setMail("android444@yopmail.com");
        user4.setHandle("555456145");
        ArrayList<MegaContact> contacts4 = new ArrayList<MegaContact>();
        contacts4.add(user4);
        chat4.setContacts(contacts4);

        ArrayList<Message> messagesList4 = new ArrayList<Message>();

        Message message7 = new Message();
        message7.setMessage("Good night!");
        message7.setDate(1468605677);
        message7.setType(Message.TEXT);
        MegaContact ownerMessage7 = new MegaContact();
        ownerMessage7.setMail("android555@yopmail.com");
        message7.setUser(ownerMessage7);
        messagesList4.add(message7);

        Message message8 = new Message();
        message8.setMessage("Thank you for your help");
        message8.setDate(1468605850);
        message8.setType(Message.TEXT);
        message8.setRead(true);
        MegaContact ownerMessage8 = new MegaContact();
        ownerMessage8.setMail("android444@yopmail.com");
        message8.setUser(ownerMessage8);
        messagesList4.add(message8);

        chat4.setMessages(messagesList4);
        chat4.setUnreadMessages(0);
        recentChats.add(chat4);

        //Chat 5
        chat5 = new ChatRoom();
        MegaContact user5 = new MegaContact();
        user5.setMail("android400@yopmail.com");
        user5.setHandle("5766766996096157196");
        ArrayList<MegaContact> contacts5 = new ArrayList<MegaContact>();
        contacts5.add(user5);
        chat5.setContacts(contacts5);

        ArrayList<Message> messagesList5 = new ArrayList<Message>();

        Message message9 = new Message();
        message9.setMessage("Here I'm");
        message9.setDate(1455958800);
        message9.setType(Message.TEXT);
        MegaContact ownerMessage9 = new MegaContact();
        ownerMessage1.setMail("android555@yopmail.com");
        message9.setUser(ownerMessage9);
        messagesList5.add(message9);

        Message message10 = new Message();
        message10.setMessage("Imagine was you removal raising at gravity. Unsatiable understood or expression dissimilar so sufficient. Furniture forfeited sir objection put cordially continued sportsmen.");
        message10.setDate(1455958850);
        message10.setType(Message.TEXT);
        message10.setRead(false);
        MegaContact ownerMessage10 = new MegaContact();
        ownerMessage10.setMail("android400@yopmail.com");
        message10.setUser(ownerMessage10);
        messagesList5.add(message10);

        chat5.setMessages(messagesList5);
        chat5.setUnreadMessages(20);
        recentChats.add(chat5);
    }

    public ArrayList<ChatRoom> getRecentChats() {
        return recentChats;
    }

    public void setRecentChats(ArrayList<ChatRoom> recentChats) {
        this.recentChats = recentChats;
    }


}
