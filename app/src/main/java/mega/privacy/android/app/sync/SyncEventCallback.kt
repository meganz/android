package mega.privacy.android.app.sync

import mega.privacy.android.app.DatabaseHandler
import mega.privacy.android.app.MegaApplication
import mega.privacy.android.app.utils.LogUtil.logDebug
import nz.mega.sdk.MegaApiJava
import nz.mega.sdk.MegaError
import nz.mega.sdk.MegaRequest

/**
 * Callback when backup related requests finished.
 */
interface SyncEventCallback {

    /**
     * @return Request type of the callback handle with.
     */
    fun requestType(): Int

    /**
     * Callback when request success.
     */
    fun onSuccess(
        api: MegaApiJava,
        request: MegaRequest,
        error: MegaError
    )

    /**
     * Callback when request failed.
     */
    fun onFail(
        api: MegaApiJava,
        request: MegaRequest,
        error: MegaError
    ) {
        // default empty implementation
    }

    /**
     * @return DatabaseHandler object.
     */
    fun getDatabase(): DatabaseHandler = MegaApplication.getInstance().dbH
}

