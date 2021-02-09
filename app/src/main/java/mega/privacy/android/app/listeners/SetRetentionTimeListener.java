package mega.privacy.android.app.listeners;

import android.content.Context;

import nz.mega.sdk.MegaChatApiJava;
import nz.mega.sdk.MegaChatError;
import nz.mega.sdk.MegaChatRequest;

import static mega.privacy.android.app.utils.LogUtil.*;

public class SetRetentionTimeListener extends ChatBaseListener {

    public SetRetentionTimeListener(Context context) {
        super(context);
    }

    @Override
    public void onRequestFinish(MegaChatApiJava api, MegaChatRequest request, MegaChatError e) {
        if (request.getType() != MegaChatRequest.TYPE_SET_RETENTION_TIME) return;

        if (e.getErrorCode() == MegaChatError.ERROR_OK) {
            logDebug("Establish the retention time successfully");
        } else {
            logError("Error setting retention time: " + e.getErrorString());
        }
    }
}
