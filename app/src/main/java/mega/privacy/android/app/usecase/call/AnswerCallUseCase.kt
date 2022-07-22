package mega.privacy.android.app.usecase.call

import io.reactivex.rxjava3.core.Single
import mega.privacy.android.app.MegaApplication
import mega.privacy.android.app.listeners.OptionalMegaChatRequestListenerInterface
import mega.privacy.android.app.utils.CallUtil
import mega.privacy.android.app.utils.Constants.START_CALL_AUDIO_ENABLE
import mega.privacy.android.app.utils.permission.PermissionUtils.hasPermissions
import nz.mega.sdk.MegaChatApiAndroid
import nz.mega.sdk.MegaChatApiJava.MEGACHAT_INVALID_HANDLE
import nz.mega.sdk.MegaChatError
import nz.mega.sdk.MegaChatRequest
import nz.mega.sdk.MegaError
import timber.log.Timber
import android.Manifest
import mega.privacy.android.app.meeting.answerChatCall
import mega.privacy.android.app.usecase.exception.toMegaException
import javax.inject.Inject

/**
 * Use case to answer call
 *
 * @property megaChatApi    MegaChatApi required to call the SDK
 */
class AnswerCallUseCase @Inject constructor(
    private val megaChatApi: MegaChatApiAndroid,
) {
    /**
     * Call Result.
     *
     * @property chatHandle       Chat ID
     * @property enableVideo      Video ON
     * @property enableAudio      Audio ON
     */
    data class AnswerCallResult(
        val chatHandle: Long = MEGACHAT_INVALID_HANDLE,
        val enableVideo: Boolean = false,
        val enableAudio: Boolean = false,
    )

    /**
     * Method to answer a call
     *
     * @param chatId Chat ID
     * @param enableVideo True, video ON. False, video OFF
     * @param enableAudio True, audio ON. False, audio OFF
     * @param enableSpeaker True, speaker ON. False, speaker OFF
     */
    fun answerCall(
        chatId: Long,
        enableVideo: Boolean,
        enableAudio: Boolean,
        enableSpeaker: Boolean,
    ): Single<AnswerCallResult> =
        Single.create { emitter ->
            if (CallUtil.amIParticipatingInThisMeeting(chatId)) {
                Timber.d("Already participating in this call")
                return@create
            }

            if (MegaApplication.getChatManagement().isAlreadyJoiningCall(chatId)) {
                Timber.d("The call has been answered")
                return@create
            }

            var audio = enableAudio
            if (audio) {
                audio = hasPermissions(MegaApplication.getInstance().applicationContext,
                    Manifest.permission.RECORD_AUDIO)
            }

            var video = enableVideo
            if (video) {
                video = hasPermissions(MegaApplication.getInstance().applicationContext,
                    Manifest.permission.CAMERA)
            }

            MegaApplication.getChatManagement().addJoiningCallChatId(chatId)

            answerChatCall(
                megaChatApi = megaChatApi,
                chatId = chatId,
                enableVideo = video,
                enableAudio = audio,
                listener = OptionalMegaChatRequestListenerInterface(
                    onRequestFinish = { request: MegaChatRequest, error: MegaChatError ->
                        if (emitter.isDisposed) return@OptionalMegaChatRequestListenerInterface

                        val requestChatId = request.chatHandle
                        MegaApplication.getChatManagement().removeJoiningCallChatId(chatId)
                        if (error.errorCode == MegaError.API_OK) {
                            Timber.d("Call answered")
                            CallUtil.addChecksForACall(chatId, enableSpeaker)

                            megaChatApi.getChatCall(requestChatId)?.let { call ->
                                MegaApplication.getChatManagement()
                                    .setRequestSentCall(call.callId, false)
                            }

                            val enabledAudio: Boolean = request.paramType == START_CALL_AUDIO_ENABLE
                            val enabledVideo = request.flag
                            emitter.onSuccess(
                                AnswerCallResult(
                                    requestChatId,
                                    enabledVideo,
                                    enabledAudio
                                )
                            )
                        } else {
                            MegaApplication.getInstance().removeRTCAudioManagerRingIn()
                            megaChatApi.getChatCall(request.chatHandle)?.let { call ->
                                CallUtil.clearIncomingCallNotification(call.callId)
                            }
                            emitter.onError(error.toMegaException())
                        }
                    })
            )
        }
}