package mega.privacy.android.app.listeners;

import android.content.Context;

import mega.privacy.android.app.activities.ManageChatHistoryActivity;
import nz.mega.sdk.MegaChatApiJava;
import nz.mega.sdk.MegaChatRoom;

public class UpdatePreviewersListener extends ChatRoomBaseListener{
    public UpdatePreviewersListener(Context context) {
        super(context);
    }

    @Override
    public void onChatRoomUpdate(MegaChatApiJava api, MegaChatRoom chat) {
        if(chat.hasChanged(MegaChatRoom.CHANGE_TYPE_UPDATE_PREVIEWERS)){
            if(context instanceof ManageChatHistoryActivity){

            }
        }
    }
}
