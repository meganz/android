package mega.privacy.android.app.listeners

import android.content.Intent
import mega.privacy.android.app.AuthenticityCredentialsActivity
import mega.privacy.android.app.MegaApplication
import mega.privacy.android.app.constants.BroadcastConstants
import nz.mega.sdk.MegaApiJava
import nz.mega.sdk.MegaError
import nz.mega.sdk.MegaRequest
import nz.mega.sdk.MegaRequestListenerInterface

/**
 * Listener to verify Authenticity credentials
 *
 * @param activity : AuthenticityCredentialsActivity
 */
class VerifyCredentialsListener(private val activity: AuthenticityCredentialsActivity) :
    MegaRequestListenerInterface {

    /**
     * Callback function for onRequestStart
     *
     * @param api : MegaApiJava
     * @param request : MegaRequest
     */
    override fun onRequestStart(api: MegaApiJava, request: MegaRequest) {
        if (request.type == MegaRequest.TYPE_VERIFY_CREDENTIALS) {
            MegaApplication.setVerifyingCredentials(true)
        }
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
        if (request.type == MegaRequest.TYPE_VERIFY_CREDENTIALS) {
            MegaApplication.setVerifyingCredentials(false)
            activity.sendBroadcast(Intent(BroadcastConstants.BROADCAST_ACTION_INTENT_FILTER_CONTACT_UPDATE)
                .setAction(BroadcastConstants.ACTION_UPDATE_CREDENTIALS)
                .putExtra(BroadcastConstants.EXTRA_USER_HANDLE, request.nodeHandle))
            activity.finishVerifyCredentialsAction(request, e)
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