package mega.privacy.android.app.meeting

import android.view.View
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import mega.privacy.android.app.utils.VideoCaptureUtils
import nz.mega.sdk.MegaChatApiAndroid
import nz.mega.sdk.MegaChatRequestListenerInterface

/**
 * Extension for bottom sheet dialog fragment
 * set click listener for view, after that, will close fragment
 *
 * @param view the target view
 * @param action the listener
 */
fun BottomSheetDialogFragment.listenAction(view: View, action: () -> Unit) {
    view.setOnClickListener {
        action()
        dismiss()
    }
}

/**
 * Delegation method to start a chat call.
 * FIXME the business logic in this method should be moved to use case in domain
 *
 * @param megaChatApi MegaChat api instance
 * @param chatid MegaChatHandle that identifies the chat room
 * @param enableVideo True for audio-video call, false for audio call
 * @param enableAudio True for starting a call with audio (mute disabled)
 * @param listener MegaChatRequestListener to track this request
 */
fun startChatCall(
    megaChatApi: MegaChatApiAndroid,
    chatId: Long,
    enableVideo: Boolean,
    enableAudio: Boolean,
    listener: MegaChatRequestListenerInterface,
) {
    // Always try to start the call using the front camera
    val frontCamera = VideoCaptureUtils.getFrontCamera()
    if (frontCamera != null) {
        megaChatApi.setChatVideoInDevice(frontCamera, null)
    }

    megaChatApi.startChatCall(chatId, enableVideo, enableAudio, listener)
}

/**
 * Delegation method to answer a chat call.
 * FIXME the business logic in this method should be moved to use case in domain layer
 *
 * @param megaChatApi MegaChat api instance
 * @param chatId MegaChatHandle that identifies the chat room
 * @param enableVideo True for audio-video call, false for audio call
 * @param enableAudio True for answering a call with audio (mute disabled)
 * @param listener MegaChatRequestListener to track this request
 */
fun answerChatCall(
    megaChatApi: MegaChatApiAndroid,
    chatId: Long,
    enableVideo: Boolean,
    enableAudio: Boolean,
    listener: MegaChatRequestListenerInterface,
) {
    // Always try to start the call using the front camera
    val frontCamera = VideoCaptureUtils.getFrontCamera()
    if (frontCamera != null) {
        megaChatApi.setChatVideoInDevice(frontCamera, null)
    }

    megaChatApi.answerChatCall(
        chatId,
        enableVideo,
        enableAudio,
        listener)
}
