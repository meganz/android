package mega.privacy.android.app.data.repository

import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import mega.privacy.android.app.domain.entity.*
import mega.privacy.android.app.domain.repository.ChatRepository
import nz.mega.sdk.*
import javax.inject.Inject

class DefaultChatRepository @Inject constructor(
    private val chatApi: MegaChatApiAndroid
) : ChatRepository {
    override fun notifyChatLogout(): Flow<Boolean> {
        return callbackFlow {
            val listener = object : MegaChatRequestListenerInterface{
                override fun onRequestStart(api: MegaChatApiJava?, request: MegaChatRequest?) {}

                override fun onRequestUpdate(api: MegaChatApiJava?, request: MegaChatRequest?) {}

                override fun onRequestFinish(
                    api: MegaChatApiJava?,
                    request: MegaChatRequest?,
                    e: MegaChatError?
                ) {
                   if (request?.type == MegaChatRequest.TYPE_LOGOUT){
                       if (e?.errorCode == MegaError.API_OK){
                           trySend(true)
                       }
                   }
                }

                override fun onRequestTemporaryError(
                    api: MegaChatApiJava?,
                    request: MegaChatRequest?,
                    e: MegaChatError?
                ) {}
            }
            chatApi.addChatRequestListener(listener)

            awaitClose{ chatApi.removeChatRequestListener(listener) }
        }
    }
    override suspend fun getUnreadNotificationCount(): Int {
        return chatApi.unreadChats
    }

    override fun getUnreadNotificationCountChanges(): Flow<Int> {
        return callbackFlow {

            val listener = object : MegaChatListenerInterface {
                override fun onChatListItemUpdate(api: MegaChatApiJava?, item: MegaChatListItem?) {
                    val unreadCount =
                        item?.takeIf { it.hasChanged(MegaChatListItem.CHANGE_TYPE_UNREAD_COUNT) }?.unreadCount

                    unreadCount?.let { trySend(it) }
                }

                override fun onChatInitStateUpdate(api: MegaChatApiJava?, newState: Int) {}

                override fun onChatOnlineStatusUpdate(
                    api: MegaChatApiJava?,
                    userhandle: Long,
                    status: Int,
                    inProgress: Boolean
                ) {
                }

                override fun onChatPresenceConfigUpdate(
                    api: MegaChatApiJava?,
                    config: MegaChatPresenceConfig?
                ) {
                }

                override fun onChatConnectionStateUpdate(
                    api: MegaChatApiJava?,
                    chatid: Long,
                    newState: Int
                ) {
                }

                override fun onChatPresenceLastGreen(
                    api: MegaChatApiJava?,
                    userhandle: Long,
                    lastGreen: Int
                ) {
                }
            }

            chatApi.addChatListener(listener)

            awaitClose { chatApi.removeChatListener(listener) }
        }
    }

    override suspend fun getNumberOfCalls(): Int {
        return chatApi.numCalls
    }

    override fun monitorCallStateChanges(): Flow<CallStateChange> {
        return callbackFlow {
            val listener = object : MegaChatCallListenerInterface {
                override fun onChatCallUpdate(api: MegaChatApiJava?, call: MegaChatCall?) {
                    call?.let {
                        if (it.hasChanged(MegaChatCall.CHANGE_TYPE_STATUS)) {
                            trySend(CallStatusChange(it.callId, mapCallStatusFromInt(it.status)))
                        }
                        if (it.hasChanged(MegaChatCall.CHANGE_TYPE_LOCAL_AVFLAGS)) {
                            trySend(LocalAvflagsChange(it.callId))
                        }
                        if (it.hasChanged(MegaChatCall.CHANGE_TYPE_RINGING_STATUS)) {
                            trySend(RingingStatusChange(it.callId))
                        }
                        if (it.hasChanged(MegaChatCall.CHANGE_TYPE_CALL_COMPOSITION)) {
                            trySend(CallCompositionChange(it.callId))
                        }
                        if (it.hasChanged(MegaChatCall.CHANGE_TYPE_CALL_ON_HOLD)) {
                            trySend(CallOnHoldChange(it.callId))
                        }
                        if (it.hasChanged(MegaChatCall.CHANGE_TYPE_CALL_SPEAK)) {
                            trySend(CallSpeakChange(it.callId))
                        }
                        if (it.hasChanged(MegaChatCall.CHANGE_TYPE_AUDIO_LEVEL)) {
                            trySend(AudioLevelChange(it.callId))
                        }
                        if (it.hasChanged(MegaChatCall.CHANGE_TYPE_NETWORK_QUALITY)) {
                            trySend(NetworkQualityChange(it.callId))
                        }
                    }


                }

                override fun onChatSessionUpdate(
                    api: MegaChatApiJava?,
                    chatid: Long,
                    callid: Long,
                    session: MegaChatSession?
                ) {
                }

            }

            chatApi.addChatCallListener(listener)

            awaitClose { chatApi.removeChatCallListener(listener) }
        }
    }


    private fun mapCallStatusFromInt(status: Int): CallStatus {
        return when (status) {
            MegaChatCall.CALL_STATUS_INITIAL -> CallStatus.Initial
            MegaChatCall.CALL_STATUS_USER_NO_PRESENT -> CallStatus.UserNoPresent
            MegaChatCall.CALL_STATUS_CONNECTING -> CallStatus.Connecting
            MegaChatCall.CALL_STATUS_JOINING -> CallStatus.Joining
            MegaChatCall.CALL_STATUS_IN_PROGRESS -> CallStatus.InProgress
            MegaChatCall.CALL_STATUS_TERMINATING_USER_PARTICIPATION -> CallStatus.TerminatingUserParticipation
            MegaChatCall.CALL_STATUS_DESTROYED -> CallStatus.Destroyed
            else -> CallStatus.Unknown
        }
    }

    override suspend fun getCallCountByState(callStatus: CallStatus): Long {
        return chatApi.getChatCalls(mapCallStateToInt(callStatus)).size()
    }

    private fun mapCallStateToInt(callStatus: CallStatus): Int {
        return when (callStatus) {
            CallStatus.Initial -> MegaChatCall.CALL_STATUS_INITIAL
            CallStatus.UserNoPresent -> MegaChatCall.CALL_STATUS_USER_NO_PRESENT
            CallStatus.Connecting -> MegaChatCall.CALL_STATUS_CONNECTING
            CallStatus.Joining -> MegaChatCall.CALL_STATUS_JOINING
            CallStatus.InProgress -> MegaChatCall.CALL_STATUS_IN_PROGRESS
            CallStatus.TerminatingUserParticipation -> MegaChatCall.CALL_STATUS_TERMINATING_USER_PARTICIPATION
            CallStatus.Destroyed -> MegaChatCall.CALL_STATUS_DESTROYED
            CallStatus.Unknown -> -1
        }
    }

}