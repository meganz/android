package mega.privacy.android.app.listeners;

import android.content.Context;

import mega.privacy.android.app.OpenPasswordLinkActivity;
import nz.mega.sdk.MegaApiJava;
import nz.mega.sdk.MegaError;
import nz.mega.sdk.MegaRequest;
public class PasswordLinkListener extends BaseListener{

    public PasswordLinkListener(Context context) {
        super(context);
    }

    @Override
    public void onRequestFinish(MegaApiJava api, MegaRequest request, MegaError e) {
        if (request.getType() != MegaRequest.TYPE_PASSWORD_LINK) return;

        if (context instanceof OpenPasswordLinkActivity) {
            ((OpenPasswordLinkActivity) context).managePasswordLinkRequest(e, request.getText());
        }
    }
}
