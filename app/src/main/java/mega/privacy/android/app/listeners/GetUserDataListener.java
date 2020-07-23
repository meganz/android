package mega.privacy.android.app.listeners;

import android.content.Context;

import nz.mega.sdk.MegaApiJava;
import nz.mega.sdk.MegaError;
import nz.mega.sdk.MegaRequest;

/**
 * Refresh user data, for example, verified phone number.
 */
public class GetUserDataListener extends BaseListener {

    private OnUserDataUpdateCallback callback;

    public GetUserDataListener(Context context, OnUserDataUpdateCallback callback) {
        super(context);
        this.callback = callback;
    }

    @Override
    public void onRequestFinish(MegaApiJava api, MegaRequest request, MegaError e) {
        if (request.getType() != MegaRequest.TYPE_GET_USER_DATA) return;
        if (callback != null) {
            callback.onUserDataUpdate(e);
        }
    }

    public interface OnUserDataUpdateCallback {

        void onUserDataUpdate(MegaError e);
    }
}
