package mega.privacy.android.app.listeners

import android.content.Context
import mega.privacy.android.app.R
import mega.privacy.android.app.interfaces.ActionNodeCallback
import mega.privacy.android.app.utils.LogUtil.logWarning
import mega.privacy.android.app.utils.StringResourcesUtils.getString
import mega.privacy.android.app.utils.Util.showSnackbar
import nz.mega.sdk.MegaApiJava
import nz.mega.sdk.MegaError
import nz.mega.sdk.MegaError.API_OK
import nz.mega.sdk.MegaRequest
import nz.mega.sdk.MegaRequest.TYPE_RENAME

class RenameListener(context: Context) : BaseListener(context) {

    private var isMyChatFilesFolder: Boolean = false
    private var actionNodeCallback: ActionNodeCallback? = null

    constructor (context: Context, isMyChatFilesFolder: Boolean) : this(context) {
        this.isMyChatFilesFolder = isMyChatFilesFolder
    }

    constructor (context: Context, actionNodeCallback: ActionNodeCallback?) : this(context) {
        this.actionNodeCallback = actionNodeCallback
    }

    override fun onRequestFinish(api: MegaApiJava, request: MegaRequest, e: MegaError) {
        if (request.type != TYPE_RENAME) {
            return
        }

        when {
            e.errorCode == API_OK -> {
                showSnackbar(context, getString(R.string.context_correctly_renamed))
                actionNodeCallback?.finishRenameActionWithSuccess()
            }
            isMyChatFilesFolder -> {
                logWarning("Error renaming \"My chat files\" folder")
            }
            else -> {
                showSnackbar(context, getString(R.string.context_no_renamed))
            }
        }
    }
}