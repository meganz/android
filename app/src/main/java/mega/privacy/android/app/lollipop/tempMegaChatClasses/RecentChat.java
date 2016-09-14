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
    ChatRoom chat6;

    public RecentChat(){

        recentChats = new ArrayList<ChatRoom>();

        //Chat one
        chat1 = new ChatRoom();
        chat1.setId(0);
        chat1.setMute(false);
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
        message2.setMessage("Let's go!!");
        message2.setDate(1472123750);
        message2.setType(Message.TEXT);
        message2.setRead(false);
        MegaContact ownerMessage2 = new MegaContact();
        ownerMessage2.setMail("android111@yopmail.com");
        message2.setUser(ownerMessage2);
        messagesList1.add(message2);

        Message message23 = new Message();
        message23.setMessage("Thank you!I'm good! How is Jess? Ar you still in town?");
//        message23.setDate(1472637592);//Wednesday
        message23.setDate(1472810392);//Today
        message23.setType(Message.TEXT);
        message23.setRead(false);
        message23.setUser(ownerMessage2);
        messagesList1.add(message23);

        chat1.setMessages(messagesList1);
        chat1.setUnreadMessages(3);
        recentChats.add(chat1);

        //Chat 2
        chat2 = new ChatRoom();
        chat2.setId(1);
        chat2.setMute(false);
        MegaContact user2 = new MegaContact();
        user2.setMail("android222@yopmail.com");
        user2.setHandle("1883490498849024631");
        ArrayList<MegaContact> contacts2 = new ArrayList<MegaContact>();
        contacts2.add(user2);
        chat2.setContacts(contacts2);

        ArrayList<Message> messagesList2 = new ArrayList<Message>();

        Message message3 = new Message();
        message3.setMessage("Hi! How are you?");
        message3.setDate(1471724060);
        message3.setType(Message.TEXT);
        message3.setRead(true);
        MegaContact ownerMessage3 = new MegaContact();
        ownerMessage3.setMail("android222@yopmail.com");
        message3.setUser(ownerMessage3);
        messagesList2.add(message3);

        Message message13 = new Message();
        message13.setMessage("I’m good. Thanks! And you?");
        message13.setDate(1471880152); //22 august
        message13.setType(Message.TEXT);
        message13.setRead(true);
        MegaContact ownerMessage4 = new MegaContact();
        ownerMessage4.setMail("android555@yopmail.com");
        message13.setUser(ownerMessage4);
        messagesList2.add(message13);

        Message message17 = new Message();
        message17.setMessage("Nothing new, but I heard you have good news");
        message17.setDate(1471880200); //22 august
        message17.setType(Message.TEXT);
        message17.setRead(true);
        message17.setUser(ownerMessage3);
        messagesList2.add(message17);

        Message message20 = new Message();
        message20.setMessage("TELL me, plz");
        message20.setDate(1471880202); //22 august
        message20.setType(Message.TEXT);
        message20.setRead(true);
        message20.setUser(ownerMessage3);
        messagesList2.add(message20);

        Message message18 = new Message();
        message18.setMessage("I have an appointment with a new editor. Fingers crossed!");
        message18.setDate(1471880302); //22 august
        message18.setType(Message.TEXT);
        message18.setRead(true);
        message18.setUser(ownerMessage4);
        messagesList2.add(message18);

        Message message19 = new Message();
        message19.setMessage("REALLY?? WHen??");
        message19.setDate(1471982500); //22 august
        message19.setType(Message.TEXT);
        message19.setRead(true);
        message19.setUser(ownerMessage3);
        messagesList2.add(message19);

        Message message14 = new Message();
        message14.setMessage("Next week. Wednesday. I sent the manuscript a year ago...");
        message14.setDate(1471982563); //23 august
        message14.setType(Message.TEXT);
        message14.setRead(true);
        message14.setUser(ownerMessage4);
        messagesList2.add(message14);

        Message message21 = new Message();
        message21.setMessage("Good luck!!! I want you to tell me everything as you leave, okay?");
        message21.setDate(1471982600); //22 august
        message21.setType(Message.TEXT);
        message21.setRead(true);
        message21.setUser(ownerMessage3);
        messagesList2.add(message21);

        Message message15 = new Message();
        message15.setMessage("Thanks!");
        message15.setDate(1471982600); //23 august
        message15.setType(Message.TEXT);
        message15.setRead(true);
        message15.setUser(ownerMessage4);
        messagesList2.add(message15);

        Message message16 = new Message();
        message16.setMessage("Many Thanks!");
        message16.setDate(1471982620); //23 august
        message16.setType(Message.TEXT);
        message16.setRead(true);
        message16.setUser(ownerMessage4);
        messagesList2.add(message16);

        Message message4 = new Message();
        message4.setMessage("Have a great night!");
        message4.setDate(1472741992);//Yesterday
        message4.setType(Message.TEXT);
        message4.setRead(true);
        message4.setUser(ownerMessage3);
        messagesList2.add(message4);

        chat2.setMessages(messagesList2);
        chat2.setUnreadMessages(0);
        recentChats.add(chat2);

        //Chat 3
        chat3 = new ChatRoom();
        chat3.setId(2);
        chat3.setMute(true);
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

        //Chat 5
        chat5 = new ChatRoom();
        chat5.setId(3);
        chat5.setMute(false);
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
        message10.setDate(1468605850);
        message10.setType(Message.TEXT);
        message10.setRead(false);
        MegaContact ownerMessage10 = new MegaContact();
        ownerMessage10.setMail("android400@yopmail.com");
        message10.setUser(ownerMessage10);
        messagesList5.add(message10);

        chat5.setMessages(messagesList5);
        chat5.setUnreadMessages(20);
        recentChats.add(chat5);

        //Chat 4
        chat4 = new ChatRoom();
        chat4.setId(4);
        chat4.setMute(false);
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
        message8.setDate(1455958850);
        message8.setType(Message.TEXT);
        message8.setRead(true);
        MegaContact ownerMessage8 = new MegaContact();
        ownerMessage8.setMail("android444@yopmail.com");
        message8.setUser(ownerMessage8);
        messagesList4.add(message8);

        Message message22= new Message();
        message22.setMessage("Trying date comparator");
        message22.setRead(true);
        message22.setDate(1472205592);//26 agosto
        message22.setType(Message.TEXT);
        message22.setDuration(8432);
        message22.setUser(ownerMessage7);
        messagesList4.add(message22);

        chat4.setMessages(messagesList4);
        chat4.setUnreadMessages(0);
        recentChats.add(chat4);

        //Chat 6
        chat6 = new ChatRoom();
        chat6.setId(5);
        chat6.setMute(true);
        MegaContact user6 = new MegaContact();
        user6.setMail("megaiostest@yopmail.com");
        user6.setHandle("-3079530489669586212");
        ArrayList<MegaContact> contacts6= new ArrayList<MegaContact>();
        contacts6.add(user6);
        chat6.setContacts(contacts6);

        ArrayList<Message> messagesList6 = new ArrayList<Message>();

        Message message11 = new Message();
        message11.setMessage("I need a chat plz");
        message11.setDate(1470070055);
        message11.setType(Message.TEXT);
        MegaContact ownerMessage11 = new MegaContact();
        ownerMessage11.setMail("android555@yopmail.com");
        message11.setUser(ownerMessage11);
        messagesList6.add(message11);

        Message message12 = new Message();
        message12.setDate(1470070068);
        message12.setType(Message.VIDEO);
        message12.setDuration(8432);
        MegaContact ownerMessage12 = new MegaContact();
        ownerMessage12.setMail("megaiostest@yopmail.com");
        message12.setUser(ownerMessage12);
        messagesList6.add(message12);

        chat6.setMessages(messagesList6);
        chat6.setUnreadMessages(0);
        recentChats.add(chat6);

        //Chat 7
        ChatRoom chat7;
        chat7 = new ChatRoom();
        chat7.setId(2012);
        chat7.setMute(false);
        MegaContact user7 = new MegaContact();
        user7.setMail("android666@yopmail.com");
        user7.setHandle("7008151781064668195");
        ArrayList<MegaContact> contacts7= new ArrayList<MegaContact>();
        contacts7.add(user7);
        chat7.setContacts(contacts7);

        ArrayList<Message> messagesList7 = new ArrayList<Message>();

        Message message30 = new Message();
        message30.setMessage("I need a call plz");
        message30.setDate(1473847303);
        message30.setType(Message.TEXT);
        MegaContact ownerMessage30 = new MegaContact();
        ownerMessage30.setMail("android555@yopmail.com");
        message30.setUser(ownerMessage30);
        messagesList7.add(message30);

        Message message31 = new Message();
        message31.setDate(1473847437);
        message31.setType(Message.VIDEO);
        message31.setDuration(40);
        MegaContact ownerMessage31 = new MegaContact();
        ownerMessage31.setMail("android666@yopmail.com");
        message31.setUser(ownerMessage31);
        messagesList7.add(message31);

        chat7.setMessages(messagesList7);
        chat7.setUnreadMessages(0);
        recentChats.add(chat7);
    }

    public ArrayList<ChatRoom> getRecentChats() {
        return recentChats;
    }

    public void setRecentChats(ArrayList<ChatRoom> recentChats) {
        this.recentChats = recentChats;
    }


}
