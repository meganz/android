package mega.privacy.android.app.usecase.call

import io.reactivex.rxjava3.core.Single
import mega.privacy.android.app.listeners.OptionalMegaChatRequestListenerInterface
import mega.privacy.android.app.usecase.toMegaException
import mega.privacy.android.app.utils.Constants.START_CALL_AUDIO_ENABLE
import nz.mega.sdk.MegaChatApiAndroid
import nz.mega.sdk.MegaChatApiJava.MEGACHAT_INVALID_HANDLE
import nz.mega.sdk.MegaChatError
import nz.mega.sdk.MegaChatRequest
import nz.mega.sdk.MegaError
import javax.inject.Inject

/**
 * Use case to start call
 *
 * @property megaChatApi    MegaChatApi required to call the SDK
 */
class StartCallUseCase @Inject constructor(
    private val megaChatApi: MegaChatApiAndroid
) {

    /**
     * Call Result.
     *
     * @property chatHandle       Chat ID
     * @property enableVideo      Video ON
     * @property enableAudio      Audio ON
     */
    data class StartCallResult(
        val chatHandle: Long = MEGACHAT_INVALID_HANDLE,
        val enableVideo: Boolean = false,
        val enableAudio: Boolean = false,
    )

    fun startCall(
        chatId: Long,
        enableVideo: Boolean,
        enableAudio: Boolean
    ): Single<StartCallResult> =
        Single.create { emitter ->
            megaChatApi.startChatCall(
                chatId,
                enableVideo,
                enableAudio,
                OptionalMegaChatRequestListenerInterface(
                    onRequestFinish = { request: MegaChatRequest, error: MegaChatError ->
                        if (emitter.isDisposed) return@OptionalMegaChatRequestListenerInterface
                        val chatID = request.chatHandle

                        if (error.errorCode == MegaError.API_OK) {
                            val enabledAudio: Boolean = request.paramType == START_CALL_AUDIO_ENABLE
                            val enabledVideo = request.flag
                            emitter.onSuccess(
                                StartCallResult(
                                    chatID,
                                    enabledVideo,
                                    enabledAudio
                                )
                            )
                        } else {
                            emitter.onError(error.toMegaException())
                        }
                    })
            )
        }
}