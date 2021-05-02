package mega.privacy.android.app.meeting.listeners

import android.content.Context
import mega.privacy.android.app.listeners.ChatBaseListener
import mega.privacy.android.app.utils.LogUtil
import nz.mega.sdk.*

class RequestHiResVideoListener(context: Context?) : ChatBaseListener(context) {

    override fun onRequestFinish(api: MegaChatApiJava, request: MegaChatRequest, e: MegaChatError) {
        if (request.type != MegaChatRequest.TYPE_REQUEST_HIGH_RES_VIDEO) {
            return
        }

        if (e.errorCode == MegaError.API_OK) {
            LogUtil.logDebug("Request high res video: chatId = "+request.chatHandle+", hires? "+request.flag+", clientId = "+request.userHandle)
        } else {
            LogUtil.logError("Error Request high res video")
        }
    }
}