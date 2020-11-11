package mega.privacy.android.app.listeners;

import android.content.Context;
import android.content.Intent;

import mega.privacy.android.app.MegaApplication;
import nz.mega.sdk.MegaApiJava;
import nz.mega.sdk.MegaError;
import nz.mega.sdk.MegaRequest;

import static mega.privacy.android.app.constants.BroadcastConstants.ACTION_SET_VERSION_INFO_SETTING;

public class FolderInfoListener extends BaseListener {

    public FolderInfoListener(Context context) {
        super(context);
    }

    @Override
    public void onRequestFinish(MegaApiJava api, MegaRequest request, MegaError e) {
        super.onRequestFinish(api, request, e);
        if (context == null)
            return;

        switch (request.getType()) {
            case MegaRequest.TYPE_FOLDER_INFO:
                MegaApplication.getInstance().sendBroadcast(new Intent(ACTION_SET_VERSION_INFO_SETTING));
                break;
        }
    }
}
