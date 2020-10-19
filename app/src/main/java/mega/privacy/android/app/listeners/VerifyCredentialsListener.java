package mega.privacy.android.app.listeners;

import android.content.Context;
import android.content.Intent;

import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import mega.privacy.android.app.AuthenticityCredentialsActivity;
import mega.privacy.android.app.MegaApplication;
import nz.mega.sdk.MegaApiJava;
import nz.mega.sdk.MegaError;
import nz.mega.sdk.MegaRequest;

import static mega.privacy.android.app.constants.BroadcastConstants.*;

public class VerifyCredentialsListener extends BaseListener {

    public VerifyCredentialsListener(Context context) {
        super(context);
    }

    @Override
    public void onRequestStart(MegaApiJava api, MegaRequest request) {
        if (request.getType() != MegaRequest.TYPE_VERIFY_CREDENTIALS) return;

        MegaApplication.setVerifyingCredentials(true);
    }

    @Override
    public void onRequestFinish(MegaApiJava api, MegaRequest request, MegaError e) {
        if (request.getType() != MegaRequest.TYPE_VERIFY_CREDENTIALS) return;

        MegaApplication.setVerifyingCredentials(false);

        context.sendBroadcast(new Intent(BROADCAST_ACTION_INTENT_FILTER_CONTACT_UPDATE)
                .setAction(ACTION_UPDATE_CREDENTIALS)
                .putExtra(EXTRA_USER_HANDLE, request.getNodeHandle()));

        if (context instanceof AuthenticityCredentialsActivity) {
            ((AuthenticityCredentialsActivity) context).finishVerifyCredentialsAction(request, e);
        }
    }
}
