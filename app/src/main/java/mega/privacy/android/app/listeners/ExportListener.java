package mega.privacy.android.app.listeners;

import android.content.Context;
import android.content.Intent;

import nz.mega.sdk.MegaApiJava;
import nz.mega.sdk.MegaError;
import nz.mega.sdk.MegaRequest;

import static mega.privacy.android.app.utils.LogUtil.*;
import static mega.privacy.android.app.utils.MegaNodeUtil.*;

public class ExportListener extends BaseListener {

    private Intent shareIntent;

    public ExportListener(Context context) {
        super(context);
    }

    /**
     * Constructor used for the purpose of launch a view intent to share content through the link created when the request finishes
     *
     * @param context       current Context
     * @param shareIntent   Intent to share the content
     */
    public ExportListener(Context context, Intent shareIntent) {
        super(context);

        this.shareIntent = shareIntent;
    }

    @Override
    public void onRequestFinish(MegaApiJava api, MegaRequest request, MegaError e) {
        if (request.getType() != MegaRequest.TYPE_EXPORT) return;

        if (e.getErrorCode() == MegaError.API_OK && request.getLink() != null) {
            if (shareIntent != null) {
                startShareIntent(context, shareIntent, request.getLink());
            }
        } else {
            logError("Error exporting node: " + e.getErrorString());
        }
    }
}
