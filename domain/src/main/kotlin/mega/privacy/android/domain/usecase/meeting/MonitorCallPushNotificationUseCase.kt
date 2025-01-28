package mega.privacy.android.domain.usecase.meeting

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.merge
import mega.privacy.android.domain.entity.call.ChatCallChanges
import mega.privacy.android.domain.entity.call.ChatCallStatus
import mega.privacy.android.domain.entity.meeting.CallPushMessageNotificationActionType
import mega.privacy.android.domain.entity.meeting.FakeIncomingCallState
import mega.privacy.android.domain.qualifier.DefaultDispatcher
import mega.privacy.android.domain.repository.CallRepository
import mega.privacy.android.domain.repository.ContactsRepository
import mega.privacy.android.domain.usecase.call.IsChatStatusConnectedForCallUseCase
import javax.inject.Inject

/**
 * Monitor call push notifications
 *
 * @property callRepository                 [CallRepository]
 * @property defaultDispatcher              [CoroutineDispatcher]

 */
class MonitorCallPushNotificationUseCase @Inject constructor(
    private val callRepository: CallRepository,
    private val setFakeIncomingCallUseCase: SetFakeIncomingCallStateUseCase,
    private val setPendingToHangUpCallUseCase: SetPendingToHangUpCallUseCase,
    private val contactsRepository: ContactsRepository,
    private val isChatStatusConnectedForCallUseCase: IsChatStatusConnectedForCallUseCase,
    @DefaultDispatcher private val defaultDispatcher: CoroutineDispatcher,
) {

    /**
     * Invoke
     *
     * @return Flow of Push notification actions
     */
    operator fun invoke(): Flow<MutableMap<Long, CallPushMessageNotificationActionType>> = flow {
        emitAll(
            merge(
                monitorFakeIncomingCallUpdates(),
                monitorChatConnectionStateUpdates(),
                monitorChatCallUpdates()
            )
        )
    }.flowOn(defaultDispatcher)

    private fun monitorFakeIncomingCallUpdates(): Flow<MutableMap<Long, CallPushMessageNotificationActionType>> =
        callRepository.monitorFakeIncomingCall()
            .map { map ->
                val result = emptyMap<Long, CallPushMessageNotificationActionType>().toMutableMap()
                map.entries.forEach {
                    val chatId = it.key
                    val type = it.value
                    when (type) {
                        FakeIncomingCallState.Notification -> result[chatId] =
                            CallPushMessageNotificationActionType.Show

                        FakeIncomingCallState.Screen -> result[chatId] =
                            CallPushMessageNotificationActionType.Hide

                        FakeIncomingCallState.Dismiss -> result[chatId] =
                            CallPushMessageNotificationActionType.Remove

                        FakeIncomingCallState.Remove -> {
                            result[chatId] = CallPushMessageNotificationActionType.Remove
                            setFakeIncomingCallUseCase(chatId = chatId, type = null)
                        }
                    }
                }
                result
            }

    private fun monitorChatConnectionStateUpdates(): Flow<MutableMap<Long, CallPushMessageNotificationActionType>> =
        contactsRepository.monitorChatConnectionStateUpdates()
            .map { chatConnectionState ->
                val chatId = chatConnectionState.chatId
                val result = emptyMap<Long, CallPushMessageNotificationActionType>().toMutableMap()
                val isConnected = isChatStatusConnectedForCallUseCase(chatId = chatId)
                val isNotification =
                    callRepository.getFakeIncomingCall(chatId = chatId) == FakeIncomingCallState.Notification
                if (isConnected && isNotification) {
                    val call = callRepository.getChatCall(chatId)
                    if (call == null) {
                        result[chatId] = CallPushMessageNotificationActionType.Missed
                        setFakeIncomingCallUseCase(chatId = chatId, type = null)
                    } else {
                        result[chatId] = CallPushMessageNotificationActionType.Update
                    }
                }
                result
            }

    private fun monitorChatCallUpdates(): Flow<MutableMap<Long, CallPushMessageNotificationActionType>> =
        callRepository.monitorChatCallUpdates()
            .map { call ->
                val result = emptyMap<Long, CallPushMessageNotificationActionType>().toMutableMap()
                when {
                    call.changes.orEmpty().contains(ChatCallChanges.Status) -> {
                        val chatId = call.chatId
                        val callId = call.callId

                        val isPendingToHangUp = callRepository.isPendingToHangUp(chatId)

                        when (call.status) {
                            ChatCallStatus.UserNoPresent -> {
                                if (isPendingToHangUp) {
                                    setPendingToHangUpCallUseCase(chatId = chatId, add = false)
                                    callRepository.hangChatCall(callId)
                                } else {
                                    val isNotification =
                                        callRepository.getFakeIncomingCall(chatId = chatId) == FakeIncomingCallState.Notification
                                    val isConnected =
                                        isChatStatusConnectedForCallUseCase(chatId = chatId)
                                    if (isConnected && isNotification) {
                                        result[chatId] =
                                            CallPushMessageNotificationActionType.Update
                                    }
                                }
                            }

                            ChatCallStatus.Joining,
                            ChatCallStatus.InProgress,
                            ChatCallStatus.TerminatingUserParticipation,
                            ChatCallStatus.Destroyed,
                                -> {
                                if (isPendingToHangUp) {
                                    setPendingToHangUpCallUseCase(chatId = chatId, add = false)
                                }
                                setFakeIncomingCallUseCase(
                                    chatId = chatId,
                                    type = FakeIncomingCallState.Remove
                                )
                            }

                            else -> {}
                        }

                    }
                }

                result
            }
}