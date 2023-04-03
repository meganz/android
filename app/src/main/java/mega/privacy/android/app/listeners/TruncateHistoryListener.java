package mega.privacy.android.app.listeners;


import android.content.Context;

import androidx.annotation.NonNull;

import mega.privacy.android.app.R;
import mega.privacy.android.app.main.megachat.ChatActivity;
import mega.privacy.android.app.utils.Util;
import nz.mega.sdk.MegaChatApiJava;
import nz.mega.sdk.MegaChatError;
import nz.mega.sdk.MegaChatRequest;
import nz.mega.sdk.MegaError;
import timber.log.Timber;

public class TruncateHistoryListener extends ChatBaseListener {

    public TruncateHistoryListener(Context context) {
        super(context);
    }

    @Override
    public void onRequestFinish(@NonNull MegaChatApiJava api, MegaChatRequest request, @NonNull MegaChatError e) {
        if (request.getType() != MegaChatRequest.TYPE_TRUNCATE_HISTORY) return;

        if (e.getErrorCode() == MegaError.API_OK) {
            Util.showSnackbar(context, context.getString(R.string.clear_history_success));
            if (context instanceof ChatActivity) {
                ((ChatActivity) context).hideScrollToLastMsgButton();
            }
        } else {
            Util.showSnackbar(context, context.getString(R.string.clear_history_error));
            Timber.e("Error clearing history: %s", e.getErrorString());
        }
    }
}