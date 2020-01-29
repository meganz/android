package mega.privacy.android.app.listeners;

import android.content.Context;

import mega.privacy.android.app.R;
import nz.mega.sdk.MegaApiJava;
import nz.mega.sdk.MegaError;
import nz.mega.sdk.MegaRequest;

import static mega.privacy.android.app.utils.Constants.*;
import static mega.privacy.android.app.utils.Util.*;
import static nz.mega.sdk.MegaApiJava.INVALID_HANDLE;

public class ShareListener extends BaseListener {
    public static final String SHARE_LISTENER = "SHARE_LISTENER";
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

        switch (typeShare) {
            case SHARE_LISTENER:
                if (numberPendingRequests == 0) {
                    String message;
                    if (numberErrors == 0) {
                        message = context.getString(R.string.context_correctly_shared);
                    } else {
                        message = context.getString(R.string.context_no_shared_number, numberErrors);
                    }
                    showSnackBar(context, SNACKBAR_TYPE, message, (int) INVALID_HANDLE);
                }
                break;

            case REMOVE_SHARE_LISTENER:
                if (numberPendingRequests == 0) {
                    String message;
                    if (numberErrors == 0) {
                        message = context.getString(R.string.context_share_correctly_removed);
                    } else {
                        message = context.getString(R.string.context_no_removed_shared);
                    }
                    showSnackBar(context, SNACKBAR_TYPE, message, (int) INVALID_HANDLE);
                }
                break;
        }
    }
}
