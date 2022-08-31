package mega.privacy.android.app.listeners

import android.content.Context
import android.content.Intent
import android.net.Uri
import mega.privacy.android.app.OpenLinkActivity
import mega.privacy.android.app.utils.StringResourcesUtils
import mega.privacy.android.app.utils.Util
import nz.mega.sdk.MegaApiJava
import nz.mega.sdk.MegaError
import nz.mega.sdk.MegaRequest
import nz.mega.sdk.MegaRequestListenerInterface
import timber.log.Timber

/**
 * Listener for session transfer
 *
 * @param context : Context
 */
class SessionTransferURLListener(private val context: Context) : MegaRequestListenerInterface {

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
        if (request.type == MegaRequest.TYPE_GET_SESSION_TRANSFER_URL) {
            if (e.errorCode == MegaError.API_OK) {
                request.link?.let { requestLink ->
                    Uri.parse(requestLink)?.let { uri ->
                        (context as? OpenLinkActivity)?.openWebLink(requestLink)
                            ?: context.startActivity(Intent(Intent.ACTION_VIEW, uri))
                    } ?: run {
                        Timber.e("Error MegaRequest.TYPE_GET_SESSION_TRANSFER_URL: link cannot be parsed")
                    }
                } ?: run {
                    Timber.e("Error MegaRequest.TYPE_GET_SESSION_TRANSFER_URL: link is NULL")
                }
            } else {
                Timber.e("Error MegaRequest.TYPE_GET_SESSION_TRANSFER_URL: %s", e.errorString)
                Util.showSnackbar(context, StringResourcesUtils.getTranslatedErrorString(e))
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