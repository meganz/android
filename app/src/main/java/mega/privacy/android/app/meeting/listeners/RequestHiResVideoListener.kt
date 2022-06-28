package mega.privacy.android.app.meeting.listeners

import android.content.Context
import mega.privacy.android.app.listeners.ChatBaseListener
import nz.mega.sdk.MegaChatApiJava
import nz.mega.sdk.MegaChatError
import nz.mega.sdk.MegaChatRequest
import nz.mega.sdk.MegaError
import timber.log.Timber

class RequestHiResVideoListener(context: Context?) : ChatBaseListener(context) {

    override fun onRequestFinish(api: MegaChatApiJava, request: MegaChatRequest, e: MegaChatError) {
        if (request.type != MegaChatRequest.TYPE_REQUEST_HIGH_RES_VIDEO) {
            return
        }

        if (e.errorCode == MegaError.API_OK) {
            Timber.d("Request high res video: chatId = ${request.chatHandle}, hires? ${request.flag}, clientId = ${request.userHandle}")
        } else {
            Timber.e("Error Request high res video. Error code ${e.errorCode}")
        }
    }
}