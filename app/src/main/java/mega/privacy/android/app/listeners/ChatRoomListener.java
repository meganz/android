package mega.privacy.android.app.listeners;

import static mega.privacy.android.app.constants.BroadcastConstants.ACTION_UPDATE_RETENTION_TIME;
import static mega.privacy.android.app.constants.BroadcastConstants.BROADCAST_ACTION_UPDATE_HISTORY_BY_RT;
import static mega.privacy.android.app.constants.BroadcastConstants.RETENTION_TIME;
import static mega.privacy.android.app.constants.EventConstants.EVENT_CHAT_OPEN_INVITE;
import static mega.privacy.android.app.constants.EventConstants.EVENT_CHAT_TITLE_CHANGE;
import static mega.privacy.android.app.utils.Constants.MESSAGE_ID;
import static nz.mega.sdk.MegaChatRoom.CHANGE_TYPE_OPEN_INVITE;
import static nz.mega.sdk.MegaChatRoom.CHANGE_TYPE_TITLE;

import android.content.Intent;

import com.jeremyliao.liveeventbus.LiveEventBus;

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

        if (chat.hasChanged(CHANGE_TYPE_TITLE)) {
            Timber.d("CHANGE_TYPE_TITLE for the chat: %s", chat.getChatId());
            LiveEventBus.get(EVENT_CHAT_TITLE_CHANGE, MegaChatRoom.class).post(chat);
        }

        if (chat.hasChanged(CHANGE_TYPE_OPEN_INVITE)) {
            LiveEventBus.get(EVENT_CHAT_OPEN_INVITE, MegaChatRoom.class).post(chat);
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
        if (msg != null) {
            Intent intentRetentionTime = new Intent(BROADCAST_ACTION_UPDATE_HISTORY_BY_RT);
            intentRetentionTime.putExtra(MESSAGE_ID, msg.getMsgId());
            intentRetentionTime.setPackage(MegaApplication.getInstance().getApplicationContext().getPackageName());
            MegaApplication.getInstance().sendBroadcast(intentRetentionTime);
        }
    }
}
