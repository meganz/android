package mega.privacy.android.app.lollipop.megachat;

import mega.privacy.android.app.MegaContactAdapter;
import nz.mega.sdk.MegaChatListItem;

public class ChatExplorerListItem {

    MegaContactAdapter contact;
    MegaChatListItem chat;
    String title;

    public ChatExplorerListItem (MegaContactAdapter contact) {
        this.contact = contact;
        this.chat = null;
        this.title = contact.getFullName();
    }
    public ChatExplorerListItem (MegaChatListItem chat) {
        this.contact = null;
        this.chat = chat;
        this.title = chat.getTitle();
    }

    public ChatExplorerListItem (MegaChatListItem chat, MegaContactAdapter contact) {
        this.contact = contact;
        this.chat = chat;
        this.title = chat.getTitle();
    }

    public MegaChatListItem getChat() {
        return chat;
    }

    public MegaContactAdapter getContact() {
        return contact;
    }

    public String getTitle() {
        return title;
    }
}
