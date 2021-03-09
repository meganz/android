package mega.privacy.android.app.listeners

import android.content.Context
import mega.privacy.android.app.R
import mega.privacy.android.app.interfaces.SnackbarShower
import mega.privacy.android.app.interfaces.showSnackbar
import mega.privacy.android.app.utils.LogUtil.logWarning
import mega.privacy.android.app.utils.StringResourcesUtils.getString
import nz.mega.sdk.MegaApiJava
import nz.mega.sdk.MegaError
import nz.mega.sdk.MegaRequest

class RenameNodeListener(
    private val snackbarShower: SnackbarShower?,
    context: Context,
    private val showSnackbar: Boolean = true,
    private val isMyChatFilesFolder: Boolean = false,
) :
    BaseListener(context) {
    override fun onRequestFinish(api: MegaApiJava, request: MegaRequest, e: MegaError) {
        if (request.type != MegaRequest.TYPE_MOVE) {
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

        if (isMyChatFilesFolder && e.errorCode != MegaError.API_OK) {
            logWarning("Error renaming \"My chat files\" folder")
        }
    }
}
