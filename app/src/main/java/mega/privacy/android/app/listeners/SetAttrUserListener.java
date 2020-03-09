package mega.privacy.android.app.listeners;

import android.content.Context;

import mega.privacy.android.app.DatabaseHandler;
import mega.privacy.android.app.MegaApplication;
import mega.privacy.android.app.R;
import nz.mega.sdk.MegaApiJava;
import nz.mega.sdk.MegaError;
import nz.mega.sdk.MegaRequest;

import static mega.privacy.android.app.utils.LogUtil.*;
import static mega.privacy.android.app.utils.Util.*;
import static nz.mega.sdk.MegaApiJava.*;
import static mega.privacy.android.app.utils.ContactUtil.*;

public class SetAttrUserListener extends BaseListener {
    DatabaseHandler dbH;

    public SetAttrUserListener(Context context) {
        super(context);
        dbH = MegaApplication.getInstance().getDbH();
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
            case USER_ATTR_FIRSTNAME:
                if (e.getErrorCode() == MegaError.API_OK) {
                    updateFirstName(context, dbH, request.getText(), request.getEmail());
                }
                break;
            case USER_ATTR_LASTNAME:
                if (e.getErrorCode() == MegaError.API_OK) {
                    updateLastName(context, dbH, request.getText(), request.getEmail());
                }
                break;
            case USER_ATTR_ALIAS:
                if (e.getErrorCode() == MegaError.API_OK) {
                    String nickname = request.getText();
                    dbH.setContactNickname(nickname, request.getNodeHandle());
                    String message;
                    if (request.getText() == null) {
                        message = context.getString(R.string.snackbar_nickname_removed);
                    } else {
                        message = context.getString(R.string.snackbar_nickname_added);
                    }
                    showSnackbar(context, message);
                    notifyNicknameUpdate(context, request.getNodeHandle());
                } else if (e.getErrorCode() == MegaError.API_ENOENT) {
                    dbH.setContactNickname(null, request.getNodeHandle());
                    notifyNicknameUpdate(context, request.getNodeHandle());
                } else {
                    logError("Error adding, updating or removing the alias" + e.getErrorCode());
                }
                break;
        }
    }
}
