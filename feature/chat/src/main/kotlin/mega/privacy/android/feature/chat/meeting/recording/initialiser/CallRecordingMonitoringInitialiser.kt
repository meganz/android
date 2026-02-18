package mega.privacy.android.feature.chat.meeting.recording.initialiser

import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import mega.privacy.android.domain.entity.call.CallRecordingConsentStatus
import mega.privacy.android.domain.usecase.call.BroadcastCallRecordingConsentEventUseCase
import mega.privacy.android.domain.usecase.call.MonitorCallRecordingConsentEventUseCase
import mega.privacy.android.domain.usecase.call.MonitorRecordedChatsUseCase
import mega.privacy.android.feature.chat.navigation.CallRecordingConsentDialogNavKey
import mega.privacy.android.navigation.contract.initialisation.initialisers.PostLoginInitialiser
import mega.privacy.android.navigation.contract.queue.dialog.AppDialogEvent
import mega.privacy.android.navigation.contract.queue.dialog.AppDialogsEventQueue
import javax.inject.Inject

/**
 * Call recording monitoring initialiser
 *
 * @property monitorRecordedChatsUseCase
 * @property appDialogEventQueue
 * @property monitorCallRecordingConsentEventUseCase
 * @property broadcastCallRecordingConsentEventUseCase
 */
class CallRecordingMonitoringInitialiser @Inject constructor(
    private val monitorRecordedChatsUseCase: MonitorRecordedChatsUseCase,
    private val appDialogEventQueue: AppDialogsEventQueue,
    private val monitorCallRecordingConsentEventUseCase: MonitorCallRecordingConsentEventUseCase,
    private val broadcastCallRecordingConsentEventUseCase: BroadcastCallRecordingConsentEventUseCase,
) : PostLoginInitialiser {

    override suspend fun invoke(session: String, isFastLogin: Boolean) {
        combine(
            monitorCallRecordingConsentEventUseCase(),
            monitorRecordedChatsUseCase(),
        ) { callRecordingConsentStatus, callRecordingEvent ->
            callRecordingConsentStatus to callRecordingEvent
        }.collectLatest { (callRecordingConsentStatus, callRecordingEvent) ->
            if (callRecordingEvent.isSessionOnRecording.not()) {
                if (callRecordingConsentStatus != CallRecordingConsentStatus.None) {
                    broadcastCallRecordingConsentEventUseCase(CallRecordingConsentStatus.None)
                }
            } else {
                val recordingEventChatId = callRecordingEvent.chatId
                when (callRecordingConsentStatus) {
                    is CallRecordingConsentStatus.Denied -> {
                        if (callRecordingConsentStatus.chatId != recordingEventChatId) {
                            requestConsent(recordingEventChatId)
                        }
                    }

                    is CallRecordingConsentStatus.Granted -> {
                        if (callRecordingConsentStatus.chatId != recordingEventChatId) {
                            requestConsent(recordingEventChatId)
                        }
                    }

                    CallRecordingConsentStatus.None -> {
                        requestConsent(recordingEventChatId)
                    }

                    is CallRecordingConsentStatus.Pending -> {
                        requestConsent(
                            recordingEventChatId,
                            callRecordingConsentStatus.chatId != recordingEventChatId
                        )
                    }

                    is CallRecordingConsentStatus.Requested -> {
                        if (callRecordingConsentStatus.chatId != recordingEventChatId) {
                            requestConsent(recordingEventChatId)
                        }
                    }
                }
            }
        }
    }

    private suspend fun requestConsent(recordingEventChatId: Long, updateConsent: Boolean = true) {
        appDialogEventQueue.emit(
            AppDialogEvent(CallRecordingConsentDialogNavKey(recordingEventChatId))
        )
        if (updateConsent) {
            broadcastCallRecordingConsentEventUseCase(
                CallRecordingConsentStatus.Pending(
                    recordingEventChatId
                )
            )
        }
    }

}