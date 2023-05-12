package mega.privacy.android.app.listeners

import android.content.Context
import android.content.Intent
import mega.privacy.android.app.MegaApplication
import mega.privacy.android.app.R
import mega.privacy.android.app.constants.BroadcastConstants
import mega.privacy.android.app.interfaces.SnackbarShower
import mega.privacy.android.app.interfaces.showSnackbar
import mega.privacy.android.app.utils.StringResourcesUtils.getTranslatedErrorString
import nz.mega.sdk.MegaApiJava
import nz.mega.sdk.MegaError
import nz.mega.sdk.MegaRequest
import nz.mega.sdk.MegaRequestListenerInterface

/**
 * RemoveListener
 * @property snackbarShower: SnackbarShower
 * @property isIncomingShare: Boolean
 * @property onFinish: Lambda
 */
class RemoveListener(
    private val snackbarShower: SnackbarShower? = null,
    private val isIncomingShare: Boolean = false,
    private val context: Context,
    private val onFinish: ((Boolean) -> Unit)? = null,
) : MegaRequestListenerInterface {

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
        if (request.type == MegaRequest.TYPE_REMOVE) {
            if (isIncomingShare) {
                if (e.errorCode == MegaError.API_OK) {
                    Intent(BroadcastConstants.BROADCAST_ACTION_SHOW_SNACKBAR).run {
                        putExtra(
                            BroadcastConstants.SNACKBAR_TEXT,
                            context.resources.getQuantityString(
                                R.plurals.shared_items_incoming_shares_snackbar_leaving_shares_success,
                                1, 1
                            )
                        )
                        MegaApplication.getInstance().sendBroadcast(this)
                    }
                } else {
                    snackbarShower?.showSnackbar(getTranslatedErrorString(e))
                }

                return
            }
            onFinish?.invoke(e.errorCode == MegaError.API_OK)
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
}
