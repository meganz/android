package mega.privacy.android.app.listeners;

import android.content.Context;

import nz.mega.sdk.MegaChatApiJava;
import nz.mega.sdk.MegaChatError;
import nz.mega.sdk.MegaChatRequest;
import timber.log.Timber;

public class SetRetentionTimeListener extends ChatBaseListener {

    public SetRetentionTimeListener(Context context) {
        super(context);
    }

    @Override
    public void onRequestFinish(MegaChatApiJava api, MegaChatRequest request, MegaChatError e) {
        if (request.getType() != MegaChatRequest.TYPE_SET_RETENTION_TIME) return;

        if (e.getErrorCode() == MegaChatError.ERROR_OK) {
            Timber.d("Establish the retention time successfully");
        } else {
            Timber.e("Error setting retention time: %s", e.getErrorString());
        }
    }
}
