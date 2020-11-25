package mega.privacy.android.app.listeners;

import android.content.Context;
import android.content.Intent;

import mega.privacy.android.app.MegaApplication;
import nz.mega.sdk.MegaChatApiJava;
import nz.mega.sdk.MegaChatRoom;

import static mega.privacy.android.app.constants.BroadcastConstants.*;

public class RetentionTimeListener extends ChatRoomBaseListener {

    public RetentionTimeListener(Context context) {
        super(context);
    }

    @Override
    public void onChatRoomUpdate(MegaChatApiJava api, MegaChatRoom chat) {
        if (chat.hasChanged(MegaChatRoom.CHANGE_TYPE_RETENTION_TIME)) {
            Intent intentRetentionTime = new Intent(ACTION_UPDATE_RETENTION_TIME);
            intentRetentionTime.putExtra(RETENTION_TIME, chat.getRetentionTime());
            MegaApplication.getInstance().sendBroadcast(intentRetentionTime);
        }
    }
}
