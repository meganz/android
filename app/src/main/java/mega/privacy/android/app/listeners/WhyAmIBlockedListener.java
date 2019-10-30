package mega.privacy.android.app.listeners;

import android.content.Context;

import mega.privacy.android.app.MegaApplication;
import mega.privacy.android.app.WeakAccountProtectionAlertActivity;
import nz.mega.sdk.MegaApiJava;
import nz.mega.sdk.MegaError;
import nz.mega.sdk.MegaRequest;
import nz.mega.sdk.MegaRequestListenerInterface;

public class WhyAmIBlockedListener extends BaseListener {

    public WhyAmIBlockedListener(Context context) {
        super(context);
    }

    @Override
    public void onRequestFinish(MegaApiJava api, MegaRequest request, MegaError e) {
        if (request.getType() != MegaRequest.TYPE_WHY_AM_I_BLOCKED || e.getErrorCode() != MegaError.API_OK) return;

        if (context instanceof WeakAccountProtectionAlertActivity) {
            String result = String.valueOf(request.getNumber());
            ((WeakAccountProtectionAlertActivity) context).whyAmIBlockedResult(result);
        }
    }
}
