package mega.privacy.android.app.listeners

import android.content.Context
import mega.privacy.android.app.R
import mega.privacy.android.app.interfaces.ActionNodeCallback
import mega.privacy.android.app.interfaces.SnackbarShower
import mega.privacy.android.app.interfaces.showSnackbar

import nz.mega.sdk.MegaApiJava
import nz.mega.sdk.MegaError
import nz.mega.sdk.MegaRequest
import nz.mega.sdk.MegaRequestListenerInterface
import timber.log.Timber

/**
 * RenameListener
 *
 * @property snackbarShower : SnackbarShower
 * @property showSnackbar: Boolean
 * @property isMyChatFilesFolder: Boolean
 * @property actionNodeCallback: ActionNodeCallback
 */
class RenameListener(
    private val snackbarShower: SnackbarShower?,
    private val showSnackbar: Boolean = true,
    private val isMyChatFilesFolder: Boolean = false,
    private val actionNodeCallback: ActionNodeCallback?,
    private val context: Context,
) : MegaRequestListenerInterface {
    constructor(context: Context) : this(
        snackbarShower = null,
        showSnackbar = false,
        isMyChatFilesFolder = false,
        actionNodeCallback = null,
        context = context
    )

    constructor(isMyChatFilesFolder: Boolean, context: Context) : this(
        snackbarShower = null,
        showSnackbar = false,
        isMyChatFilesFolder = isMyChatFilesFolder,
        actionNodeCallback = null,
        context = context
    )

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
        if (request.type == MegaRequest.TYPE_RENAME) {
            if (showSnackbar) {
                snackbarShower?.showSnackbar(
                    context.getString(
                        if (e.errorCode == MegaError.API_OK) R.string.context_correctly_renamed
                        else R.string.context_no_renamed
                    )
                )
            }

            if (e.errorCode == MegaError.API_OK) {
                actionNodeCallback?.finishRenameActionWithSuccess(request.name)
            }

            if (isMyChatFilesFolder && e.errorCode != MegaError.API_OK) {
                Timber.w("Error renaming \"My chat files\" folder")
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
}
