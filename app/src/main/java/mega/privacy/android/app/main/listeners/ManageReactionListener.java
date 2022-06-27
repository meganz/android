package mega.privacy.android.app.main.listeners;

import static mega.privacy.android.app.utils.Constants.REACTION_ERROR_TYPE_MESSAGE;
import static mega.privacy.android.app.utils.Constants.REACTION_ERROR_TYPE_USER;

import android.content.Context;

import mega.privacy.android.app.listeners.ChatBaseListener;
import mega.privacy.android.app.main.megachat.ChatActivity;
import nz.mega.sdk.MegaChatApiJava;
import nz.mega.sdk.MegaChatError;
import nz.mega.sdk.MegaChatRequest;
import nz.mega.sdk.MegaError;
import nz.mega.sdk.MegaHandleList;
import timber.log.Timber;

public class ManageReactionListener extends ChatBaseListener {

    public ManageReactionListener(Context context) {
        super(context);
    }

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
                MegaHandleList listUsers = api.getReactionUsers(chatId, msgId, reaction);
                int count = listUsers != null ? (int) listUsers.size() : 0;
                if (context instanceof ChatActivity) {
                    ((ChatActivity) context).updateReactionAdapter(api.getMessage(chatId, msgId), reaction, count);
                }
                break;

            case MegaError.API_EEXIST:
                if (hasReactionBeenAdded) {
                    Timber.d("This reaction is already added in this message, so it should be removed");
                }
                break;

            case MegaChatError.ERROR_TOOMANY:
                long numberOfError = request.getNumber();
                if (context instanceof ChatActivity && (numberOfError == REACTION_ERROR_TYPE_USER || numberOfError == REACTION_ERROR_TYPE_MESSAGE)) {
                    ((ChatActivity) context).createLimitReactionsAlertDialog(numberOfError);
                }
                break;
        }
    }

    @Override
    public void onRequestTemporaryError(MegaChatApiJava api, MegaChatRequest request, MegaChatError e) {
    }
}
