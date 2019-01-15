package mega.privacy.android.app.lollipop.megachat;

import mega.privacy.android.app.MegaContactAdapter;
import nz.mega.sdk.MegaChatListItem;

public class ChatExplorerListItem {

    MegaContactAdapter contact;
    MegaChatListItem chat;

    public ChatExplorerListItem (MegaContactAdapter contact) {
        this.contact = contact;
        this.chat = null;
    }
    public ChatExplorerListItem (MegaChatListItem chat) {
        this.contact = null;
        this.chat = chat;
    }

    public MegaChatListItem getChat() {
        return chat;
    }

    public MegaContactAdapter getContact() {
        return contact;
    }
}
