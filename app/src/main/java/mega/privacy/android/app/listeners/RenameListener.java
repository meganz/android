package mega.privacy.android.app.listeners;

import android.content.Context;

import nz.mega.sdk.MegaApiJava;
import nz.mega.sdk.MegaError;
import nz.mega.sdk.MegaRequest;

import static mega.privacy.android.app.utils.LogUtil.*;

public class RenameListener extends BaseListener {

    private boolean isMyChatFilesFolder;

    public RenameListener(Context context) {
        super(context);
    }

    public RenameListener(Context context, boolean isMyChatFilesFolder) {
        super(context);
        this.isMyChatFilesFolder = isMyChatFilesFolder;
    }

    @Override
    public void onRequestFinish(MegaApiJava api, MegaRequest request, MegaError e) {
        if (request.getType() != MegaRequest.TYPE_RENAME) return;

        if (e.getErrorCode() != MegaError.API_OK && isMyChatFilesFolder) {
            logWarning("Error renaming \"My chat files\" folder");
            return;
        }
    }
}
