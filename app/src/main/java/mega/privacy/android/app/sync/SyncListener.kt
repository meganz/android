package mega.privacy.android.app.sync

import android.content.Context
import mega.privacy.android.app.listeners.BaseListener
import mega.privacy.android.app.utils.LogUtil.logDebug
import mega.privacy.android.app.utils.LogUtil.logWarning
import nz.mega.sdk.MegaApiJava
import nz.mega.sdk.MegaError
import nz.mega.sdk.MegaRequest

/**
 * Listen to backup related requests callback.
 */
class SyncListener(
    /**
     * Callback executes when request finished.
     */
    private val callback: SyncEventCallback,
    context: Context
) : BaseListener(context) {

    override fun onRequestFinish(api: MegaApiJava, request: MegaRequest, e: MegaError) {
        if (callback.requestType() != request.type) return

        if (e.errorCode == MegaError.API_OK) {
            logDebug("Request ${request.type}: ${request.requestString} successfully.")
            callback.onSuccess(api, request, e)
        } else {
            logWarning("Request ${request.type}: ${request.requestString} failed, ${e.errorString}: ${e.errorCode}")
            callback.onFail(api, request, e)
        }
    }
}