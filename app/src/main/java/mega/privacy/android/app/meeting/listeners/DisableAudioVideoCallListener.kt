package mega.privacy.android.app.meeting.listeners

import android.content.Context
import mega.privacy.android.app.listeners.ChatBaseListener
import nz.mega.sdk.*
import timber.log.Timber

class DisableAudioVideoCallListener(context: Context?) : ChatBaseListener(context) {

    private var callback: OnDisableAudioVideoCallback? = null

    constructor(
        context: Context?,
        callback: OnDisableAudioVideoCallback,
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
                            isEnable -> Timber.d("Audio enabled")
                            else -> Timber.d("Audio disabled")
                        }
                        callback?.onDisableAudioVideo(request.chatHandle, typeChange, isEnable)
                    }
                    MegaChatRequest.VIDEO -> {
                        when {
                            isEnable -> Timber.d("Video enabled")
                            else -> Timber.d("Video disabled")

                        }
                        callback?.onDisableAudioVideo(request.chatHandle, typeChange, isEnable)
                    }
                }
            }
            MegaChatError.ERROR_TOOMANY -> {
                when (typeChange) {
                    MegaChatRequest.AUDIO -> Timber.e("There are too many participants in the call sending audio")
                    else -> Timber.e("There are too many participants in the call sending video.")
                }
            }
            else -> Timber.e("Error disabling audio or video, errorCode ${e.errorCode}")
        }
    }

    interface OnDisableAudioVideoCallback {
        fun onDisableAudioVideo(chatId: Long, typeChange: Int, isEnable: Boolean)
    }
}