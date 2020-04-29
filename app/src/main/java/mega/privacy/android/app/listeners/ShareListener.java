package mega.privacy.android.app.listeners;

import android.content.Context;
import android.content.Intent;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import mega.privacy.android.app.MegaApplication;
import mega.privacy.android.app.R;
import nz.mega.sdk.MegaApiJava;
import nz.mega.sdk.MegaError;
import nz.mega.sdk.MegaRequest;

import static mega.privacy.android.app.constants.BroadcastConstants.*;
import static mega.privacy.android.app.utils.Util.*;

public class ShareListener extends BaseListener {
    public static final String SHARE_LISTENER = "SHARE_LISTENER";
    public static final String CHANGE_PERMISSIONS_LISTENER = "CHANGE_PERMISSIONS_LISTENER";
    public static final String REMOVE_SHARE_LISTENER = "REMOVE_SHARE_LISTENER";

    private static String typeShare;
    private int numberPendingRequests;
    private int numberErrors;

    public ShareListener(Context context, String typeShare, int numberShares) {
        super(context);

        this.typeShare = typeShare;
        this.numberPendingRequests = numberShares;
    }

    @Override
    public void onRequestFinish(MegaApiJava api, MegaRequest request, MegaError e) {
        if (request.getType() != MegaRequest.TYPE_SHARE) return;

        numberPendingRequests--;

        if (e.getErrorCode() != MegaError.API_OK) {
            numberErrors++;
        }

        String message = "";

        switch (typeShare) {
            case SHARE_LISTENER:
                if (numberPendingRequests == 0) {
                    if (numberErrors == 0) {
                        message = context.getString(R.string.context_correctly_shared);
                    } else {
                        message = context.getString(R.string.context_no_shared_number, numberErrors);
                    }
                }
                break;

            case CHANGE_PERMISSIONS_LISTENER:
                if (numberPendingRequests == 0) {
                    if (numberErrors == 0) {
                        message = context.getString(R.string.context_permissions_changed);
                    } else {
                        message = context.getString(R.string.number_permission_incorrectly_changed_from_shared, numberErrors);
                    }
                }
                break;

            case REMOVE_SHARE_LISTENER:
                if (numberPendingRequests == 0) {
                    if (numberErrors == 0) {
                        message = context.getString(R.string.context_share_correctly_removed);
                    } else {
                        message = context.getString(R.string.context_no_removed_shared);
                    }
                }
                break;
        }

        if (numberPendingRequests == 0) {
            showSnackbar(context, message);
            sendManageSharesBroadcast();
        }
    }

    private void sendManageSharesBroadcast() {
        Intent intent = new Intent(BROADCAST_ACTION_INTENT_MANAGE_SHARE);
        intent.putExtra(TYPE_SHARE, typeShare);
        LocalBroadcastManager.getInstance(MegaApplication.getInstance()).sendBroadcast(intent);

    }
}
