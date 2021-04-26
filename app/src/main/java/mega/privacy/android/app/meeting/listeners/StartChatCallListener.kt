package mega.privacy.android.app.meeting.listeners

import android.content.Context
import mega.privacy.android.app.MegaApplication
import mega.privacy.android.app.R
import mega.privacy.android.app.interfaces.SnackbarShower
import mega.privacy.android.app.interfaces.showSnackbar
import mega.privacy.android.app.listeners.ChatBaseListener
import mega.privacy.android.app.utils.LogUtil.logDebug
import mega.privacy.android.app.utils.LogUtil.logError
import mega.privacy.android.app.utils.StringResourcesUtils.getString
import nz.mega.sdk.*

class StartChatCallListener(context: Context?) : ChatBaseListener(context) {

    private var callback: OnCallStartedCallback? = null
    private var snackbarShower: SnackbarShower? = null

    constructor(
        context: Context?,
        snackbarShower: SnackbarShower,
    ) : this(context) {
        this.snackbarShower = snackbarShower
    }

    constructor(
        context: Context?,
        snackbarShower: SnackbarShower,
        callback: OnCallStartedCallback
    ) : this(context) {
        this.callback = callback
        this.snackbarShower = snackbarShower
    }

    override fun onRequestFinish(api: MegaChatApiJava, request: MegaChatRequest, e: MegaChatError) {
        if (request.type != MegaChatRequest.TYPE_START_CHAT_CALL) {
            return
        }

        if (e.errorCode == MegaError.API_OK) {
            logDebug("Call started")
            callback?.onCallStarted()
        } else {
            logError("Error Starting call")
            snackbarShower?.showSnackbar(getString(R.string.call_error))
        }
    }

    interface OnCallStartedCallback {
        fun onCallStarted()
    }
}