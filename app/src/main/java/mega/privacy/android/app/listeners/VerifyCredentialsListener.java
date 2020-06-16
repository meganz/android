package mega.privacy.android.app.listeners;

import android.content.Context;

import mega.privacy.android.app.AuthenticityCredentialsActivity;
import nz.mega.sdk.MegaApiJava;
import nz.mega.sdk.MegaError;
import nz.mega.sdk.MegaRequest;

public class VerifyCredentialsListener extends BaseListener {

    public VerifyCredentialsListener(Context context) {
        super(context);
    }

    @Override
    public void onRequestFinish(MegaApiJava api, MegaRequest request, MegaError e) {
        if (request.getType() != MegaRequest.TYPE_VERIFY_CREDENTIALS) return;

        if (context instanceof AuthenticityCredentialsActivity) {
            ((AuthenticityCredentialsActivity) context).finishVerifyCredentialsAction(request, e);
        }
    }
}
