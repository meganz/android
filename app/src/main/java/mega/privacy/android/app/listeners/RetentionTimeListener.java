package mega.privacy.android.app.listeners;

import android.content.Context;
import android.content.Intent;

import mega.privacy.android.app.MegaApplication;
import nz.mega.sdk.MegaChatApiJava;
import nz.mega.sdk.MegaChatRoom;
import static mega.privacy.android.app.constants.BroadcastConstants.*;
import static mega.privacy.android.app.utils.LogUtil.logDebug;

public class RetentionTimeListener extends ChatRoomBaseListener {

    public RetentionTimeListener(Context context) {
        super(context);
    }

    @Override
    public void onChatRoomUpdate(MegaChatApiJava api, MegaChatRoom chat) {
        if (chat.hasChanged(MegaChatRoom.CHANGE_TYPE_RETENTION_TIME)) {
            logDebug("CHANGE_TYPE_RETENTION_TIME for the chat: " + chat.getChatId());
            Intent intentRetentionTime = new Intent(ACTION_UPDATE_RETENTION_TIME);
            intentRetentionTime.putExtra(RETENTION_TIME, chat.getRetentionTime());
            MegaApplication.getInstance().sendBroadcast(intentRetentionTime);
        }
    }
}
