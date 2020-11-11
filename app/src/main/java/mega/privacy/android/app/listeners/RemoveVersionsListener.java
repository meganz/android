package mega.privacy.android.app.listeners;

import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import mega.privacy.android.app.MegaApplication;
import mega.privacy.android.app.R;
import mega.privacy.android.app.lollipop.ManagerActivityLollipop;
import mega.privacy.android.app.utils.Util;
import nz.mega.sdk.MegaApiJava;
import nz.mega.sdk.MegaError;
import nz.mega.sdk.MegaRequest;

import static mega.privacy.android.app.constants.BroadcastConstants.ACTION_RESET_VERSION_INFO_SETTING;

public class RemoveVersionsListener extends BaseListener {

    public RemoveVersionsListener(Context context) {
        super(context);
    }

    @Override
    public void onRequestFinish(MegaApiJava api, MegaRequest request, MegaError e) {
        super.onRequestFinish(api, request, e);
        if (context == null)
            return;
        switch (request.getType()) {
            case MegaRequest.TYPE_REMOVE_VERSIONS:
                if (e.getErrorCode() == MegaError.API_OK) {
                    Util.showSnackbar(context, context.getString(R.string.success_delete_versions));
                    MegaApplication.getInstance().sendBroadcast(new Intent(ACTION_RESET_VERSION_INFO_SETTING));
                    if (context instanceof ManagerActivityLollipop) {
                        final Handler handler = new Handler();
                        handler.postDelayed(() -> ((ManagerActivityLollipop) context).updateAccountStorageInfo(), 8000);
                    }
                } else {
                    Util.showSnackbar(context, context.getString(R.string.error_delete_versions));
                }
                break;
        }
    }
}