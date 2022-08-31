package mega.privacy.android.app.listeners

import android.content.Context
import mega.privacy.android.app.R
import mega.privacy.android.app.activities.settingsActivities.FileManagementPreferencesActivity
import mega.privacy.android.app.utils.DBUtil
import mega.privacy.android.app.utils.Util
import nz.mega.sdk.MegaApiJava
import nz.mega.sdk.MegaError
import nz.mega.sdk.MegaRequest
import nz.mega.sdk.MegaRequestListenerInterface

/**
 * Listener to receive clean rubbish bin updates
 *
 * @param context: Context
 */
class CleanRubbishBinListener(private val context: Context) : MegaRequestListenerInterface {

    /**
     * Callback function for onRequestStart
     *
     * @param api : MegaApiJava
     * @param request : MegaRequest
     */
    override fun onRequestStart(api: MegaApiJava?, request: MegaRequest?) {
        // Do nothing
    }

    /**
     * Callback function for onRequestUpdate
     *
     * @param api : MegaApiJava
     * @param request : MegaRequest
     */
    override fun onRequestUpdate(api: MegaApiJava?, request: MegaRequest?) {
        // Do nothing
    }

    /**
     * Callback function for onRequestFinish
     *
     * @param api : MegaApiJava
     * @param request : MegaRequest
     */
    override fun onRequestFinish(api: MegaApiJava, request: MegaRequest, e: MegaError) {
        if (request.type == MegaRequest.TYPE_CLEAN_RUBBISH_BIN) {
            context.run {
                if (e.errorCode == MegaError.API_OK) {
                    Util.showSnackbar(this, getString(R.string.rubbish_bin_emptied))
                    DBUtil.resetAccountDetailsTimeStamp()
                    (this as? FileManagementPreferencesActivity)?.resetRubbishInfo()
                } else {
                    Util.showSnackbar(this, getString(R.string.rubbish_bin_no_emptied))
                }
            }
        }
    }

    /**
     * Callback function for onRequestTemporaryError
     *
     * @param api : MegaApiJava
     * @param request : MegaRequest
     */
    override fun onRequestTemporaryError(api: MegaApiJava?, request: MegaRequest?, e: MegaError?) {
        // Do nothing
    }
}