package mega.privacy.android.app.listeners;

import android.content.Context;

import nz.mega.sdk.MegaApiJava;
import nz.mega.sdk.MegaError;
import nz.mega.sdk.MegaRequest;

import static mega.privacy.android.app.utils.LogUtil.*;
import static nz.mega.sdk.MegaApiJava.*;

public class SetAttrUserListener extends BaseListener{

    public SetAttrUserListener(Context context) {
        super(context);
    }

    @Override
    public void onRequestFinish(MegaApiJava api, MegaRequest request, MegaError e) {
        if (request.getType() != MegaRequest.TYPE_SET_ATTR_USER) return;

        switch (request.getParamType()) {
            case USER_ATTR_MY_CHAT_FILES_FOLDER:
                if (e.getErrorCode() != MegaError.API_OK) {
                    logWarning("Error setting \"My chat files\" folder as user's attribute");
                }
                break;
        }
    }
}
