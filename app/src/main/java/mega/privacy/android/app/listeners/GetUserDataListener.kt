package mega.privacy.android.app.listeners

import android.content.Context
import android.content.Intent
import mega.privacy.android.app.MegaApplication
import mega.privacy.android.app.utils.Constants
import mega.privacy.android.app.utils.LogUtil

import nz.mega.sdk.MegaApiJava
import nz.mega.sdk.MegaError
import nz.mega.sdk.MegaRequest

class GetUserDataListener(context: Context?) : BaseListener(context) {

    private var callback: OnUserDataUpdateCallback? = null

    constructor(context: Context?, callback: OnUserDataUpdateCallback) : this(context) {
        this.callback = callback
    }

    override fun onRequestFinish(api: MegaApiJava, request: MegaRequest, e: MegaError) {
        if (request.type != MegaRequest.TYPE_GET_USER_DATA) {
            return
        }

        callback?.onUserDataUpdate(e)

        if (e.errorCode == MegaError.API_OK) {
            val intent = Intent(Constants.BROADCAST_ACTION_INTENT_UPDATE_USER_DATA)
            MegaApplication.getInstance().sendBroadcast(intent)
        } else {
            LogUtil.logError("Error getting user data: " + e.errorString)
        }
    }

    interface OnUserDataUpdateCallback {
        fun onUserDataUpdate(e: MegaError?)
    }
}