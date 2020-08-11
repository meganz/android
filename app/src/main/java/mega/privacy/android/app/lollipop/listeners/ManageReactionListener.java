package mega.privacy.android.app.lollipop.listeners;

import android.content.Context;

import mega.privacy.android.app.MegaApplication;
import mega.privacy.android.app.lollipop.megachat.ChatActivityLollipop;
import nz.mega.sdk.MegaChatApiAndroid;
import nz.mega.sdk.MegaChatApiJava;
import nz.mega.sdk.MegaChatError;
import nz.mega.sdk.MegaChatRequest;
import nz.mega.sdk.MegaChatRequestListenerInterface;
import nz.mega.sdk.MegaError;
import nz.mega.sdk.MegaHandleList;

import static mega.privacy.android.app.utils.LogUtil.logDebug;

public class ManageReactionListener implements MegaChatRequestListenerInterface {

    MegaChatApiAndroid megaChatApi;
    Context context;

    public ManageReactionListener(Context context) {
        super();
        this.context = context;
        megaChatApi = MegaApplication.getInstance().getMegaChatApi();
    }

    @Override
    public void onRequestStart(MegaChatApiJava api, MegaChatRequest request) { }

    @Override
    public void onRequestUpdate(MegaChatApiJava api, MegaChatRequest request) { }

    @Override
    public void onRequestFinish(MegaChatApiJava api, MegaChatRequest request, MegaChatError e) {
        if (request.getType() != MegaChatRequest.TYPE_MANAGE_REACTION)
            return;

        long chatId = request.getChatHandle();
        long msgId = request.getUserHandle();
        String reaction = request.getText();
        boolean hasReactionBeenAdded = request.getFlag();

        switch (e.getErrorCode()) {
            case MegaChatError.ERROR_OK:
                logDebug(hasReactionBeenAdded ? "The reaction has been added correctly" : "The reaction has been deleted correctly");
                MegaHandleList listUsers = megaChatApi.getReactionUsers(chatId, msgId, reaction);
                int count = listUsers != null ? (int) listUsers.size() : 0;
                ((ChatActivityLollipop) context).updateReactionAdapter(megaChatApi.getMessage(chatId, msgId), reaction, count);
                break;

            case MegaError.API_EEXIST:
                if (hasReactionBeenAdded && !MegaApplication.isIsReactionFromKeyboard()) {
                    logDebug("This reaction is already added in this message, so it should be removed");
                    megaChatApi.delReaction(request.getChatHandle(), megaChatApi.getMessage(chatId, msgId).getMsgId(), reaction, ManageReactionListener.this);
                }
                break;
        }
    }

    @Override
    public void onRequestTemporaryError(MegaChatApiJava api, MegaChatRequest request, MegaChatError e) { }
}
