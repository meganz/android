package mega.privacy.android.app.listeners

import android.content.Context
import mega.privacy.android.app.R
import mega.privacy.android.app.interfaces.ActionNodeCallback
import mega.privacy.android.app.interfaces.SnackbarShower
import mega.privacy.android.app.interfaces.showSnackbar

import mega.privacy.android.app.utils.StringResourcesUtils.getString
import nz.mega.sdk.MegaApiJava
import nz.mega.sdk.MegaError
import nz.mega.sdk.MegaRequest
import timber.log.Timber

class RenameListener(
    private val snackbarShower: SnackbarShower?,
    context: Context,
    private val showSnackbar: Boolean = true,
    private val isMyChatFilesFolder: Boolean = false,
    private val actionNodeCallback: ActionNodeCallback?,
) : BaseListener(context) {
    constructor(context: Context) : this(null, context, false, false, null)

    constructor(context: Context, isMyChatFilesFolder: Boolean) : this(
        null, context, false, isMyChatFilesFolder, null
    )

    override fun onRequestFinish(api: MegaApiJava, request: MegaRequest, e: MegaError) {
        if (request.type != MegaRequest.TYPE_RENAME) {
            return
        }

        if (showSnackbar) {
            snackbarShower?.showSnackbar(
                getString(
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
