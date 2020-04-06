package mega.privacy.android.app.listeners;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;

import mega.privacy.android.app.lollipop.WebViewActivityLollipop;
import nz.mega.sdk.MegaApiJava;
import nz.mega.sdk.MegaError;
import nz.mega.sdk.MegaRequest;

import static mega.privacy.android.app.utils.LogUtil.*;
import static mega.privacy.android.app.utils.Util.*;

public class SessionTransferURLListener extends BaseListener {

    public SessionTransferURLListener(Context context) {
        super(context);
    }

    @Override
    public void onRequestFinish(MegaApiJava api, MegaRequest request, MegaError e) {
        if (request.getType() != MegaRequest.TYPE_GET_SESSION_TRANSFER_URL) return;

        if (e.getErrorCode() == MegaError.API_OK) {
            String link = request.getLink();
            if (link != null) {
                Uri uri = Uri.parse(link);
                if (uri != null) {
                    Intent openTermsIntent = new Intent(context, WebViewActivityLollipop.class);
                    openTermsIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                    openTermsIntent.setData(uri);
                    context.startActivity(openTermsIntent);
                    return;
                }
            }

            logError("Error MegaRequest.TYPE_GET_SESSION_TRANSFER_URL: link is NULL");
        } else {
            logError("Error MegaRequest.TYPE_GET_SESSION_TRANSFER_URL: " + e.getErrorString());
            showSnackbar(context, e.getErrorString());
        }
    }
}
