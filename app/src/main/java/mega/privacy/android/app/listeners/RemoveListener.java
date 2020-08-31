package mega.privacy.android.app.listeners;

import android.content.Context;
import android.content.Intent;

import mega.privacy.android.app.MegaApplication;
import mega.privacy.android.app.R;
import nz.mega.sdk.MegaApiJava;
import nz.mega.sdk.MegaError;
import nz.mega.sdk.MegaRequest;

import static mega.privacy.android.app.constants.BroadcastConstants.BROADCAST_ACTION_SHOW_SNACKBAR;
import static mega.privacy.android.app.constants.BroadcastConstants.SNACKBAR_TEXT;
import static mega.privacy.android.app.utils.StringResourcesUtils.getTranslatedErrorString;
import static mega.privacy.android.app.utils.Util.showSnackbar;

public class RemoveListener extends BaseListener {

    private boolean isIncomingShare;

    public RemoveListener(Context context) {
        super(context);
    }

    public RemoveListener(Context context, boolean isIncomingShare) {
        super(context);

        this.isIncomingShare = isIncomingShare;
    }

    @Override
    public void onRequestFinish(MegaApiJava api, MegaRequest request, MegaError e) {
        if (request.getType() != MegaRequest.TYPE_REMOVE) return;

        if (e.getErrorCode() == MegaError.API_OK) {
            if (isIncomingShare) {
                MegaApplication.getInstance().sendBroadcast(new Intent(BROADCAST_ACTION_SHOW_SNACKBAR)
                        .putExtra(SNACKBAR_TEXT, context.getString(R.string.share_left)));
            }
        } else {
            showSnackbar(context, getTranslatedErrorString(e));
        }
    }
}
