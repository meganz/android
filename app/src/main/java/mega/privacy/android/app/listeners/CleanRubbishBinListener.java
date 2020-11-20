package mega.privacy.android.app.listeners;

import android.content.Context;

import mega.privacy.android.app.R;
import mega.privacy.android.app.activities.settingsActivities.FileManagementPreferencesActivity;
import mega.privacy.android.app.utils.Util;
import nz.mega.sdk.MegaApiJava;
import nz.mega.sdk.MegaError;
import nz.mega.sdk.MegaRequest;

import static mega.privacy.android.app.utils.DBUtil.resetAccountDetailsTimeStamp;

public class CleanRubbishBinListener extends BaseListener {

    public CleanRubbishBinListener(Context context) {
        super(context);
    }

    @Override
    public void onRequestFinish(MegaApiJava api, MegaRequest request, MegaError e) {
        if (request.getType() != MegaRequest.TYPE_CLEAN_RUBBISH_BIN) return;

        if (e.getErrorCode() == MegaError.API_OK) {
            Util.showSnackbar(context, context.getString(R.string.rubbish_bin_emptied));
            resetAccountDetailsTimeStamp();

            if (context instanceof FileManagementPreferencesActivity) {
                ((FileManagementPreferencesActivity) context).resetRubbishInfo();
            }
        } else {
            Util.showSnackbar(context, context.getString(R.string.rubbish_bin_no_emptied));
        }
    }
}
