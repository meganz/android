package mega.privacy.android.app.listeners

import mega.privacy.android.app.presentation.weakaccountprotection.WeakAccountProtectionAlertActivity
import nz.mega.sdk.MegaApiJava
import nz.mega.sdk.MegaError
import nz.mega.sdk.MegaRequest
import nz.mega.sdk.MegaRequestListenerInterface

/**
 * Listener to get the reasons of Why Am I Blocked
 *
 * @param activity : WeakAccountProtectionAlertActivity
 */
class WhyAmIBlockedListener(private val activity: WeakAccountProtectionAlertActivity) :
    MegaRequestListenerInterface {

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
     */
    override fun onRequestFinish(api: MegaApiJava, request: MegaRequest, e: MegaError) {
        if (request.type == MegaRequest.TYPE_WHY_AM_I_BLOCKED && e.errorCode == MegaError.API_OK)
            activity.whyAmIBlockedResult(request.number.toString())
    }

    /**
     * Callback function for onRequestTemporaryError
     *
     * @param api : MegaApiJava
     * @param request : MegaRequest
     */
    override fun onRequestTemporaryError(api: MegaApiJava, request: MegaRequest, e: MegaError) {
        // Do nothing
    }
}