package mega.privacy.android.app.listeners

import android.content.Context
import mega.privacy.android.app.R
import mega.privacy.android.app.modalbottomsheet.OnSharedFolderUpdatedCallBack
import mega.privacy.android.app.utils.Util
import nz.mega.sdk.MegaApiJava
import nz.mega.sdk.MegaError
import nz.mega.sdk.MegaRequest
import nz.mega.sdk.MegaRequestListenerInterface
import mega.privacy.android.shared.resources.R as sharedR

/**
 * Listener to interact with Sharing
 *
 * @param context: Context
 * @param typeShare: Share type
 * @param numberShares: Handle for number of shares to operate
 */
class ShareListener(
    private val context: Context,
    private val typeShare: String,
    private val numberShares: Int,
) : MegaRequestListenerInterface {

    private var numberPendingRequests: Int = numberShares
    private var numberErrors = 0

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
        if (request.type == MegaRequest.TYPE_SHARE) {
            numberPendingRequests--
            if (e.errorCode != MegaError.API_OK) {
                numberErrors++
            }
            if (numberPendingRequests == 0) {
                val message = if (numberErrors == 0) {
                    getSuccessMessage(typeShare)
                } else {
                    getErrorMessage(typeShare)
                }
                Util.showSnackbar(context, message)
                (context as? OnSharedFolderUpdatedCallBack)?.onSharedFolderUpdated()
            }
        }
    }

    /**
     * Function to get success message based on typeShare if errors are zero
     *
     * @param typeShare: Type share
     * @return String: Success message string or empty string
     */
    private fun getSuccessMessage(typeShare: String): String {
        return when (typeShare) {
            SHARE_LISTENER -> {
                context.getString(R.string.context_correctly_shared)
            }

            CHANGE_PERMISSIONS_LISTENER -> {
                context.getString(R.string.context_permissions_changed)
            }

            REMOVE_SHARE_LISTENER -> {
                context.getString(sharedR.string.stop_sharing_folder_success_message)
            }

            else -> ""
        }
    }

    /**
     * Function to get error message based on typeShare in case of errors
     *
     * @param typeShare: Type share
     * @return String: error message string or empty string
     */
    private fun getErrorMessage(typeShare: String): String {
        return when (typeShare) {
            SHARE_LISTENER -> {
                context.resources.getQuantityString(
                    R.plurals.shared_items_cloud_drive_snackbar_sharing_folder_failed,
                    numberErrors, numberErrors
                )
            }

            CHANGE_PERMISSIONS_LISTENER -> {
                context.resources.getQuantityString(
                    R.plurals.shared_items_outgoing_shares_update_contact_permission_failed,
                    numberErrors, numberErrors
                )
            }

            REMOVE_SHARE_LISTENER -> {
                context.resources.getQuantityString(
                    R.plurals.shared_items_outgoing_shares_snackbar_remove_contact_access_failed,
                    numberErrors,
                    numberErrors
                )
            }

            else -> ""
        }
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

    companion object {
        /**
         * Type share constant for share listener
         */
        const val SHARE_LISTENER = "SHARE_LISTENER"

        /**
         * Type share constant for change permissions
         */
        const val CHANGE_PERMISSIONS_LISTENER = "CHANGE_PERMISSIONS_LISTENER"

        /**
         * Type share constant for remove share listener
         */
        const val REMOVE_SHARE_LISTENER = "REMOVE_SHARE_LISTENER"
    }
}