package mega.privacy.android.domain.usecase.meeting

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.merge
import mega.privacy.android.domain.entity.call.CallCompositionChanges
import mega.privacy.android.domain.entity.call.ChatCallChanges
import mega.privacy.android.domain.entity.call.ChatCallStatus
import mega.privacy.android.domain.entity.meeting.CallPushMessageNotificationActionType
import mega.privacy.android.domain.entity.meeting.FakeIncomingCallState
import mega.privacy.android.domain.qualifier.DefaultDispatcher
import mega.privacy.android.domain.repository.CallRepository
import mega.privacy.android.domain.repository.ContactsRepository
import mega.privacy.android.domain.usecase.call.IsChatStatusConnectedForCallUseCase
import mega.privacy.android.domain.usecase.contact.GetMyUserHandleUseCase
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
    private val getMyUserHandleUseCase: GetMyUserHandleUseCase,
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
            }.filter { it.isNotEmpty() }

    private fun monitorChatConnectionStateUpdates(): Flow<MutableMap<Long, CallPushMessageNotificationActionType>> =
        contactsRepository.monitorChatConnectionStateUpdates()
            .distinctUntilChanged()
            .map { chatConnectionState ->
                val chatId = chatConnectionState.chatId
                val result = emptyMap<Long, CallPushMessageNotificationActionType>().toMutableMap()
                val isConnected = isChatStatusConnectedForCallUseCase(chatId = chatId)

                if (isConnected) {
                    val call = callRepository.getChatCall(chatId)
                    val isNotification =
                        callRepository.getFakeIncomingCall(chatId = chatId) == FakeIncomingCallState.Notification

                    if (isNotification) {
                        result[chatId] =
                            when {
                                call == null -> CallPushMessageNotificationActionType.Missed
                                call.peerIdParticipants?.contains(getMyUserHandleUseCase()) == true -> CallPushMessageNotificationActionType.Remove
                                else -> CallPushMessageNotificationActionType.Update
                            }
                    }

                    if (call == null) {
                        setFakeIncomingCallUseCase(chatId = chatId, type = null)
                        setPendingToHangUpCallUseCase(chatId = chatId, add = false)
                    } else {
                        runCatching {
                            if (callRepository.isPendingToHangUp(chatId)) {
                                callRepository.hangChatCall(call.callId)
                                setPendingToHangUpCallUseCase(
                                    chatId = chatId,
                                    add = false
                                )
                            }
                        }
                    }
                }

                result
            }.filter { it.isNotEmpty() }

    private fun monitorChatCallUpdates(): Flow<MutableMap<Long, CallPushMessageNotificationActionType>> =
        callRepository.monitorChatCallUpdates()
            .map { call ->
                val result = emptyMap<Long, CallPushMessageNotificationActionType>().toMutableMap()
                call.changes?.apply {
                    if (contains(ChatCallChanges.Status) &&
                        (call.status == ChatCallStatus.Joining ||
                                call.status == ChatCallStatus.InProgress ||
                                call.status == ChatCallStatus.TerminatingUserParticipation ||
                                call.status == ChatCallStatus.Destroyed)
                    ) {
                        runCatching {
                            callRepository.isPendingToHangUp(call.callId)
                        }.onSuccess { isPendingToHangUp ->
                            if (isPendingToHangUp) {
                                setPendingToHangUpCallUseCase(chatId = call.chatId, add = false)
                            }
                            setFakeIncomingCallUseCase(
                                chatId = call.chatId,
                                type = FakeIncomingCallState.Remove
                            )
                        }
                    }
                    if ((contains(ChatCallChanges.Status) || contains(ChatCallChanges.RingingStatus)) && call.status == ChatCallStatus.UserNoPresent) {
                        val chatId = call.chatId
                        val callId = call.callId

                        runCatching {
                            if (callRepository.isPendingToHangUp(chatId)) {
                                callRepository.hangChatCall(callId)
                                setPendingToHangUpCallUseCase(
                                    chatId = chatId,
                                    add = false
                                )
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
                    }

                    if (contains(ChatCallChanges.CallComposition) &&
                        call.callCompositionChange == CallCompositionChanges.Added &&
                        call.peerIdParticipants?.contains(getMyUserHandleUseCase()) == true
                    ) {
                        result[call.chatId] =
                            CallPushMessageNotificationActionType.Remove
                    }
                }


                result
            }.filter { it.isNotEmpty() }
}