package mega.privacy.android.domain.usecase.chat

import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.flow.onStart
import mega.privacy.android.domain.entity.call.ChatCallChanges
import mega.privacy.android.domain.entity.call.ChatCallStatus
import mega.privacy.android.domain.usecase.call.GetChatCallUseCase
import mega.privacy.android.domain.usecase.meeting.MonitorChatCallUpdatesUseCase
import javax.inject.Inject

/**
 * Monitor call reconnecting status use case
 *
 */
class MonitorCallReconnectingStatusUseCase @Inject constructor(
    private val monitorChatCallUpdatesUseCase: MonitorChatCallUpdatesUseCase,
    private val getChatCallUseCase: GetChatCallUseCase,
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
     * @param chatId Chat ID
     */
    operator fun invoke(chatId: Long) = monitorChatCallUpdatesUseCase()
        .filter { it.chatId == chatId && it.changes.orEmpty().contains(ChatCallChanges.Status) }
        .onStart { getChatCallUseCase(chatId)?.let { emit(it) } }
        .mapNotNull {
            if (!reconnectingStatusList.contains(chatId)) {
                reconnectingStatusList[chatId] = ReconnectingStatusTypes.CALL_INITIATING
            }

            val reconnectingStatus: ReconnectingStatusTypes? = reconnectingStatusList[chatId]
            when (it.status) {
                ChatCallStatus.Connecting -> {
                    if (reconnectingStatus == ReconnectingStatusTypes.CALL_IN_PROGRESS) {
                        reconnectingStatusList[chatId] = ReconnectingStatusTypes.CALL_RECONNECTING
                        return@mapNotNull true
                    }
                }

                ChatCallStatus.Joining, ChatCallStatus.InProgress -> {
                    if (reconnectingStatus == ReconnectingStatusTypes.CALL_RECONNECTING) {
                        return@mapNotNull false
                    }
                    reconnectingStatusList[chatId] = ReconnectingStatusTypes.CALL_IN_PROGRESS
                }

                else -> {
                    reconnectingStatusList.remove(chatId)
                }
            }
            return@mapNotNull null
        }
}