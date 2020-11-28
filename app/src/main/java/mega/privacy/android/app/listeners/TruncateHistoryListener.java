package mega.privacy.android.app.listeners;

import android.content.Context;
import mega.privacy.android.app.R;
import mega.privacy.android.app.lollipop.megachat.ChatActivityLollipop;
import mega.privacy.android.app.utils.Util;
import nz.mega.sdk.MegaChatApiJava;
import nz.mega.sdk.MegaChatError;
import nz.mega.sdk.MegaChatRequest;
import nz.mega.sdk.MegaError;

import static mega.privacy.android.app.utils.LogUtil.logDebug;
import static mega.privacy.android.app.utils.LogUtil.logError;
import static mega.privacy.android.app.utils.StringResourcesUtils.getString;

public class TruncateHistoryListener extends ChatBaseListener {

    public TruncateHistoryListener(Context context) {
        super(context);
    }

    @Override
    public void onRequestFinish(MegaChatApiJava api, MegaChatRequest request, MegaChatError e) {
        if (request.getType() != MegaChatRequest.TYPE_TRUNCATE_HISTORY) return;

        if (e.getErrorCode() == MegaError.API_OK) {
            logDebug("Truncate history request finish.");
            Util.showSnackbar(context, getString(R.string.clear_history_success));
            if (context instanceof ChatActivityLollipop) {
                ((ChatActivityLollipop) context).hideMessageJump();
            }
        } else {
            Util.showSnackbar(context, getString(R.string.clear_history_error));
            logError("Error clearing history: " + e.getErrorString());
        }
    }
}