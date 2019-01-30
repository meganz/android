package mega.privacy.android.app.lollipop.megachat;

import mega.privacy.android.app.MegaContactAdapter;
import nz.mega.sdk.MegaChatListItem;

public class ChatExplorerListItem {

    MegaContactAdapter contact;
    MegaChatListItem chat;
    String title;
    String id;

    public ChatExplorerListItem (MegaContactAdapter contact) {
        this.contact = contact;
        this.chat = null;
        this.title = contact.getFullName();
        this.id = String.valueOf(contact.getMegaUser().getHandle());
    }
    public ChatExplorerListItem (MegaChatListItem chat) {
        this.contact = null;
        this.chat = chat;
        this.title = chat.getTitle();
        this.id = String.valueOf(chat.getChatId());
    }

    public ChatExplorerListItem (MegaChatListItem chat, MegaContactAdapter contact) {
        this.contact = contact;
        this.chat = chat;
        this.title = chat.getTitle();
        this.id = String.valueOf(chat.getChatId());
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

    public String getId() {
        return id;
    }
}
