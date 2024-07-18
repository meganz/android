package mega.privacy.android.domain.usecase.chat

import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.mapNotNull
import mega.privacy.android.domain.entity.call.ChatCallChanges
import mega.privacy.android.domain.entity.call.ChatCallStatus
import mega.privacy.android.domain.usecase.meeting.MonitorChatCallUpdatesUseCase
import javax.inject.Inject

/**
 * Get reconnecting status use case
 */
class MonitorCallsReconnectingStatusUseCase @Inject constructor(
    private val monitorChatCallUpdatesUseCase: MonitorChatCallUpdatesUseCase,
) {
    internal enum class ReconnectingStatusTypes {
        /**
         * Call initiating
         */
        CALL_INITIATING,

        /**
         * Call in progress
         */
        CALL_IN_PROGRESS,

        /**
         * Call reconnecting
         */
        CALL_RECONNECTING
    }

    private val reconnectingStatusList = hashMapOf<Long, ReconnectingStatusTypes>()

    /**
     * Invoke
     *
     */
    operator fun invoke() = monitorChatCallUpdatesUseCase()
        .filter { it.changes.orEmpty().contains(ChatCallChanges.Status) }
        .mapNotNull {
            if (!reconnectingStatusList.contains(it.chatId)) {
                reconnectingStatusList[it.chatId] = ReconnectingStatusTypes.CALL_INITIATING
            }

            val reconnectingStatus: ReconnectingStatusTypes? = reconnectingStatusList[it.chatId]
            when (it.status) {
                ChatCallStatus.Connecting -> {
                    if (reconnectingStatus == ReconnectingStatusTypes.CALL_IN_PROGRESS) {
                        reconnectingStatusList[it.chatId] =
                            ReconnectingStatusTypes.CALL_RECONNECTING
                        return@mapNotNull true
                    }
                }

                ChatCallStatus.Joining, ChatCallStatus.InProgress -> {
                    if (reconnectingStatus == ReconnectingStatusTypes.CALL_RECONNECTING) {
                        return@mapNotNull false
                    }
                    reconnectingStatusList[it.chatId] = ReconnectingStatusTypes.CALL_IN_PROGRESS
                }

                else -> {
                    reconnectingStatusList.remove(it.chatId)
                }
            }
            return@mapNotNull null
        }
}