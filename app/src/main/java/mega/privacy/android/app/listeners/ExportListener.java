package mega.privacy.android.app.listeners;

import android.content.Context;

import mega.privacy.android.app.R;
import nz.mega.sdk.MegaApiJava;
import nz.mega.sdk.MegaError;
import nz.mega.sdk.MegaRequest;

import static mega.privacy.android.app.utils.LogUtil.*;
import static mega.privacy.android.app.utils.Util.*;
import static nz.mega.sdk.MegaError.*;
import static nz.mega.sdk.MegaRequest.*;

public class ExportListener extends BaseListener {

    boolean removeExport;
    int numberRemove;
    int pendingRemove;
    int numberError;

    public ExportListener(Context context, boolean removeExport, int numberRemove) {
        super(context);
        this.removeExport = removeExport;
        this.numberRemove = this.pendingRemove = numberRemove;
    }

    @Override
    public void onRequestFinish(MegaApiJava api, MegaRequest request, MegaError e) {
        if (request.getType() != TYPE_EXPORT) return;

        if (removeExport) {
            pendingRemove--;

            if (e.getErrorCode() != API_OK) {
                numberError++;
            }

            if (pendingRemove == 0) {
                if (numberError > 0) {
                    logError("Removing link error");
                    showSnackbar(context, context.getResources().getQuantityString(R.plurals.context_link_removal_error, numberRemove));
                } else {
                    showSnackbar(context, context.getResources().getQuantityString(R.plurals.context_link_removal_success, numberRemove));
                }
            }
        }
    }
}
