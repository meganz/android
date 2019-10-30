package mega.privacy.android.app.listeners;

import android.content.Context;

import mega.privacy.android.app.R;
import mega.privacy.android.app.WeakAccountProtectionAlertActivity;
import nz.mega.sdk.MegaApiJava;
import nz.mega.sdk.MegaError;
import nz.mega.sdk.MegaRequest;
import nz.mega.sdk.MegaRequestListenerInterface;

import static mega.privacy.android.app.utils.LogUtil.*;

public class ResendVerificationEmailListener extends BaseListener {

    public ResendVerificationEmailListener(Context context) {
        super(context);
    }

    @Override
    public void onRequestFinish(MegaApiJava api, MegaRequest request, MegaError e) {
        if (request.getType() != MegaRequest.TYPE_RESEND_VERIFICATION_EMAIL) return;

        if (context instanceof WeakAccountProtectionAlertActivity) {
            if (e.getErrorCode() == MegaError.API_OK) {
                ((WeakAccountProtectionAlertActivity) context).showSnackbar(R.string.confirm_email_misspelled_email_sent);
            } else {
                logError("Error: " + e.getErrorString());
                ((WeakAccountProtectionAlertActivity) context).showSnackbar(R.string.general_error);
            }
        }
    }
}
