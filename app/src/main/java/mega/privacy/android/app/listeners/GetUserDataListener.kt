package mega.privacy.android.app.listeners

import android.content.Context
import android.content.Intent
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import mega.privacy.android.app.utils.Constants
import mega.privacy.android.app.utils.LogUtil

import nz.mega.sdk.MegaApiJava
import nz.mega.sdk.MegaError
import nz.mega.sdk.MegaRequest

class GetUserDataListener(context: Context?) : BaseListener(context) {

    override fun onRequestFinish(api: MegaApiJava, request: MegaRequest, e: MegaError) {
        if (request.type != MegaRequest.TYPE_GET_USER_DATA) {
            return
        }

        if (e.errorCode == MegaError.API_OK) {
            val intent = Intent(Constants.BROADCAST_ACTION_INTENT_UPDATE_USER_DATA)
            LocalBroadcastManager.getInstance(context).sendBroadcast(intent)
        } else {
            LogUtil.logError("Error getting user data: " + e.errorString)
        }
    }
}