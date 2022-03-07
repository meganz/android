package mega.privacy.android.app.listeners

import android.content.Context
import mega.privacy.android.app.utils.Constants.INVALID_POSITION
import mega.privacy.android.app.utils.LogUtil.logDebug
import mega.privacy.android.app.utils.LogUtil.logError
import nz.mega.sdk.MegaApiJava
import nz.mega.sdk.MegaError
import nz.mega.sdk.MegaRequest

class GetUserEmailListener(context: Context?) : BaseListener(context) {

    private var callback: OnUserEmailUpdateCallback? = null
    private var position = INVALID_POSITION

    constructor(context: Context?, callback: OnUserEmailUpdateCallback) : this(
        context
    ) {
        this.callback = callback
    }

    constructor(context: Context?, callback: OnUserEmailUpdateCallback, position: Int) : this(
        context
    ) {
        this.callback = callback
        this.position = position
    }

    override fun onRequestFinish(api: MegaApiJava, request: MegaRequest, e: MegaError) {
        if (request.type != MegaRequest.TYPE_GET_USER_EMAIL) {
            return
        }

        if (e.errorCode == MegaError.API_OK) {
            logDebug("Email recovered")
            dBH.setNonContactEmail(request.email, request.nodeHandle.toString())
            callback?.onUserEmailUpdate(request.email, request.nodeHandle, position)
        } else {
            logError("Error getting user email: " + e.errorString)
        }
    }

    interface OnUserEmailUpdateCallback {
        fun onUserEmailUpdate(email: String?, handler: Long, position: Int)
    }
}