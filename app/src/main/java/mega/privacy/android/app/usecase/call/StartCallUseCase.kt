package mega.privacy.android.app.usecase.call

import android.Manifest
import com.jeremyliao.liveeventbus.LiveEventBus
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.core.Single
import io.reactivex.rxjava3.kotlin.subscribeBy
import io.reactivex.rxjava3.schedulers.Schedulers
import mega.privacy.android.app.MegaApplication
import mega.privacy.android.app.constants.EventConstants
import mega.privacy.android.app.contacts.usecase.GetChatRoomUseCase
import mega.privacy.android.app.listeners.OptionalMegaChatRequestListenerInterface
import mega.privacy.android.app.objects.PasscodeManagement
import mega.privacy.android.app.usecase.toMegaException
import mega.privacy.android.app.utils.CallUtil
import mega.privacy.android.app.utils.Constants.START_CALL_AUDIO_ENABLE
import mega.privacy.android.app.utils.LogUtil
import mega.privacy.android.app.utils.LogUtil.logDebug
import mega.privacy.android.app.utils.permission.PermissionUtils.hasPermissions
import nz.mega.sdk.*
import nz.mega.sdk.MegaChatApiJava.MEGACHAT_INVALID_HANDLE
import timber.log.Timber
import javax.inject.Inject

/**
 * Use case to start call
 *
 * @property megaChatApi    MegaChatApi required to call the SDK
 */
class StartCallUseCase @Inject constructor(
        private val megaChatApi: MegaChatApiAndroid,
        private val passcodeManagement: PasscodeManagement,
        private val getChatRoomUseCase: GetChatRoomUseCase
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

    fun startCallFromUserHandle(userHandle: Long, enableVideo: Boolean, enableAudio: Boolean): Single<StartCallResult> =
            Single.create { emitter ->
                logDebug("****************+ startCallFromUserHandle:: Creating chat...")
                getChatRoomUseCase.get(userHandle)
                        .subscribeOn(Schedulers.io())
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribeBy(
                                onSuccess = { chatId ->
                                    Timber.d("Chat recovered")
                                    logDebug("****************+ startCallFromUserHandle:: Created chat OK && Starting call...")

                                    startCallFromChatId(chatId, enableVideo = enableVideo, enableAudio = enableAudio)
                                            .subscribeOn(Schedulers.io())
                                            .observeOn(AndroidSchedulers.mainThread())
                                            .subscribeBy(
                                                    onSuccess = { resultStartCall ->
                                                        logDebug("****************+ startCallFromUserHandle:: Started call OK")

                                                        emitter.onSuccess(resultStartCall)
                                                    },
                                                    onError = { error ->
                                                        Timber.e(error.stackTraceToString())
                                                    }
                                            )
                                },
                                onError = { error ->
                                    LogUtil.logError(error.stackTraceToString())
                                }
                        )
            }

    fun startCallFromChatId(
            chatId: Long,
            enableVideo: Boolean,
            enableAudio: Boolean
    ): Single<StartCallResult> =
            Single.create { emitter ->
                if (megaChatApi.getChatCall(chatId) != null) {
                    Timber.d("There is a call, open it")
                    CallUtil.openMeetingInProgress(MegaApplication.getInstance().applicationContext, chatId, true, passcodeManagement)
                    return@create
                }

                MegaApplication.setIsWaitingForCall(false)

                var audio = enableAudio
                if (audio) {
                    audio = hasPermissions(MegaApplication.getInstance().applicationContext, Manifest.permission.RECORD_AUDIO)
                }

                var video = enableVideo
                if (video) {
                    video = hasPermissions(MegaApplication.getInstance().applicationContext, Manifest.permission.RECORD_AUDIO) &&
                            hasPermissions(MegaApplication.getInstance().applicationContext, Manifest.permission.CAMERA)
                }
                logDebug("****************+ startCallFromChatId:: Starting call...")

                megaChatApi.startChatCall(
                        chatId,
                        video,
                        audio,
                        OptionalMegaChatRequestListenerInterface(
                                onRequestFinish = { request: MegaChatRequest, error: MegaChatError ->
                                    if (emitter.isDisposed) return@OptionalMegaChatRequestListenerInterface
                                    val chatID = request.chatHandle

                                    if (error.errorCode == MegaError.API_OK) {
                                        logDebug("****************+ startCallFromChatId:: Started call OK")

                                        Timber.d("Call started")
                                        CallUtil.addChecksForACall(request.chatHandle, request.flag)
                                        megaChatApi.getChatCall(request.chatHandle)?.let { call ->
                                            if (call.isOutgoing) {
                                                MegaApplication.getChatManagement().setRequestSentCall(call.callId, true)
                                            }
                                        }

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
                                        LiveEventBus.get(
                                                EventConstants.EVENT_ERROR_STARTING_CALL,
                                                Long::class.java
                                        ).post(request.chatHandle)
                                        emitter.onError(error.toMegaException())
                                    }
                                })
                )
            }
}