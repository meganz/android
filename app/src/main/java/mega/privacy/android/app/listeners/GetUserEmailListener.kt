package mega.privacy.android.app.listeners

import android.content.Context
import mega.privacy.android.data.database.DatabaseHandler
import mega.privacy.android.app.MegaApplication
import mega.privacy.android.app.utils.Constants.INVALID_POSITION
import nz.mega.sdk.MegaApiJava
import nz.mega.sdk.MegaError
import nz.mega.sdk.MegaRequest
import nz.mega.sdk.MegaRequestListenerInterface
import timber.log.Timber

/**
 * GetUserEmailListener
 *
 * @property context: Context
 * @property callback: OnUserEmailUpdateCallback
 * @property position Integer
 */
class GetUserEmailListener(
    val context: Context?,
    val callback: OnUserEmailUpdateCallback? = null,
    val position: Int = INVALID_POSITION,
) : MegaRequestListenerInterface {

    private val databaseHandler: DatabaseHandler by lazy { MegaApplication.getInstance().dbH }

    /**
     * Callback function for onRequestStart
     *
     * @param api : MegaApiJava
     * @param request : MegaRequest
     */
    override fun onRequestStart(api: MegaApiJava, request: MegaRequest) {
        // Do nothing
    }

    /**
     * Callback function for onRequestUpdate
     *
     * @param api : MegaApiJava
     * @param request : MegaRequest
     */
    override fun onRequestUpdate(api: MegaApiJava, request: MegaRequest) {
        // Do nothing
    }

    /**
     * Callback function for onRequestFinish
     *
     * @param api : MegaApiJava
     * @param request : MegaRequest
     * @param e: MegaError
     */
    override fun onRequestFinish(api: MegaApiJava, request: MegaRequest, e: MegaError) {
        if (request.type == MegaRequest.TYPE_GET_USER_EMAIL) {
            if (e.errorCode == MegaError.API_OK) {
                Timber.d("Email recovered")
                databaseHandler.setNonContactEmail(request.email, request.nodeHandle.toString())
                callback?.onUserEmailUpdate(request.email, request.nodeHandle, position)
            } else {
                Timber.e("Error getting user email: ${e.errorString}")
            }
        }
    }

    /**
     * Callback function for onRequestTemporaryError
     *
     * @param api : MegaApiJava
     * @param request : MegaRequest
     * @param e: MegaError
     */
    override fun onRequestTemporaryError(api: MegaApiJava, request: MegaRequest, e: MegaError) {
        // Do nothing
    }

    /**
     * OnUserEmailUpdateCallback
     */
    interface OnUserEmailUpdateCallback {

        /**
         * onUserEmailUpdate
         *
         * @param email: String
         * @param handler: Long
         * @param position: Integer
         */
        fun onUserEmailUpdate(email: String?, handler: Long, position: Int)
    }
}