package mega.privacy.android.app.listeners;

import static mega.privacy.android.app.constants.BroadcastConstants.ACTION_UPDATE_RETENTION_TIME;
import static mega.privacy.android.app.constants.BroadcastConstants.RETENTION_TIME;

import android.content.Intent;

import mega.privacy.android.app.MegaApplication;
import nz.mega.sdk.MegaChatApiJava;
import nz.mega.sdk.MegaChatMessage;
import nz.mega.sdk.MegaChatRoom;
import nz.mega.sdk.MegaChatRoomListenerInterface;
import timber.log.Timber;

public class ChatRoomListener implements MegaChatRoomListenerInterface {

    public ChatRoomListener() {
    }

    @Override
    public void onChatRoomUpdate(MegaChatApiJava api, MegaChatRoom chat) {
        if (chat.hasChanged(MegaChatRoom.CHANGE_TYPE_RETENTION_TIME)) {
            Timber.d("CHANGE_TYPE_RETENTION_TIME for the chat: %s", chat.getChatId());
            Intent intentRetentionTime = new Intent(ACTION_UPDATE_RETENTION_TIME);
            intentRetentionTime.putExtra(RETENTION_TIME, chat.getRetentionTime());
            intentRetentionTime.setPackage(MegaApplication.getInstance().getApplicationContext().getPackageName());
            MegaApplication.getInstance().sendBroadcast(intentRetentionTime);
        }
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
    public void onHistoryReloaded(MegaChatApiJava api, MegaChatRoom chat) {

    }

    @Override
    public void onReactionUpdate(MegaChatApiJava api, long msgid, String reaction, int count) {

    }

    @Override
    public void onHistoryTruncatedByRetentionTime(MegaChatApiJava api, MegaChatMessage msg) {

    }
}
