package mega.privacy.android.app.listeners

import mega.privacy.android.app.R
import mega.privacy.android.app.WeakAccountProtectionAlertActivity
import nz.mega.sdk.MegaApiJava
import nz.mega.sdk.MegaError
import nz.mega.sdk.MegaRequest
import nz.mega.sdk.MegaRequestListenerInterface

/**
 * Listener for resend verification email response
 *
 * @param activity : WeakAccountProtectionAlertActivity
 */
class ResendVerificationEmailListener(
    private val activity: WeakAccountProtectionAlertActivity,
) : MegaRequestListenerInterface {

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
        if (request.type == MegaRequest.TYPE_RESEND_VERIFICATION_EMAIL) {
            when (e.errorCode) {
                MegaError.API_OK -> {
                    activity.showSnackbar(R.string.confirm_email_misspelled_email_sent)
                }
                MegaError.API_ETEMPUNAVAIL -> {
                    activity.showSnackbar(R.string.resend_email_error)
                }
                else -> {
                    activity.showSnackbar(R.string.general_error)
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