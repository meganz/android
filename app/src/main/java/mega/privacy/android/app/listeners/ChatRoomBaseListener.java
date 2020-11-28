package mega.privacy.android.app.listeners;

import android.content.Context;

import nz.mega.sdk.MegaChatApiJava;
import nz.mega.sdk.MegaChatMessage;
import nz.mega.sdk.MegaChatRoom;
import nz.mega.sdk.MegaChatRoomListenerInterface;

public class ChatRoomBaseListener implements MegaChatRoomListenerInterface {

    protected Context context;

    public ChatRoomBaseListener(Context context) {
        this.context = context;
    }

    @Override
    public void onChatRoomUpdate(MegaChatApiJava api, MegaChatRoom chat) {
    }

    @Override
    public void onMessageLoaded(MegaChatApiJava api, MegaChatMessage msg) {

    }

    @Override
    public void onMessageReceived(MegaChatApiJava api, MegaChatMessage msg) {

    }

    @Override
    public void onMessageUpdate(MegaChatApiJava api, MegaChatMessage msg) {

    }

    @Override
    public void onHistoryTruncatedByRetentionTime(MegaChatApiJava api, MegaChatMessage msg) {

    }

    @Override
    public void onHistoryReloaded(MegaChatApiJava api, MegaChatRoom chat) {

    }

    @Override
    public void onReactionUpdate(MegaChatApiJava api, long msgid, String reaction, int count) {

    }
}
