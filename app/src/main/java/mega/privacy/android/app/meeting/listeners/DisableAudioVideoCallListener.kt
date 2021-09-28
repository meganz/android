package mega.privacy.android.app.meeting.listeners

import android.content.Context
import mega.privacy.android.app.listeners.ChatBaseListener
import mega.privacy.android.app.utils.LogUtil
import mega.privacy.android.app.utils.LogUtil.logDebug
import nz.mega.sdk.*

class DisableAudioVideoCallListener(context: Context?) : ChatBaseListener(context) {

    private var callback: OnDisableAudioVideoCallback? = null

    constructor(
        context: Context?,
        callback: OnDisableAudioVideoCallback
    ) : this(context) {
        this.callback = callback
    }

    override fun onRequestFinish(api: MegaChatApiJava, request: MegaChatRequest, e: MegaChatError) {
        if (request.type != MegaChatRequest.TYPE_DISABLE_AUDIO_VIDEO_CALL) {
            return
        }
        val typeChange = request.paramType

        when (e.errorCode) {
            MegaError.API_OK -> {
                val isEnable = request.flag
                when (typeChange) {
                    MegaChatRequest.AUDIO -> {
                        when {
                            isEnable -> logDebug("Audio enabled")
                            else -> logDebug("Audio disabled")
                        }
                        callback?.onDisableAudioVideo(request.chatHandle, typeChange, isEnable)
                    }
                    MegaChatRequest.VIDEO -> {
                        when {
                            isEnable -> logDebug("Video enabled")
                            else -> logDebug("Video disabled")

                        }
                        callback?.onDisableAudioVideo(request.chatHandle, typeChange, isEnable)
                    }
                }
            }
            MegaChatError.ERROR_TOOMANY -> {
                when (typeChange) {
                    MegaChatRequest.AUDIO -> LogUtil.logError("There are too many participants in the call sending audio")
                    else -> LogUtil.logError("There are too many participants in the call sending video.")
                }
            }
            else -> LogUtil.logError("Error disabling audio or video, errorCode " + e.errorCode)
        }
    }

    interface OnDisableAudioVideoCallback {
        fun onDisableAudioVideo(chatId: Long, typeChange: Int, isEnable: Boolean)
    }
}