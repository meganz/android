package mega.privacy.android.domain.usecase.meeting

import mega.privacy.android.domain.entity.chat.ChatCall
import mega.privacy.android.domain.entity.meeting.ChatCallStatus
import mega.privacy.android.domain.usecase.chat.GetChatCallByCallIdUseCase
import mega.privacy.android.domain.usecase.chat.GetChatCallIdsUseCase
import mega.privacy.android.domain.usecase.chat.HoldChatCallUseCase
import javax.inject.Inject

/**
 * Use case to start or join scheduled meeting
 */
class StartScheduledMeetingUseCase @Inject constructor(
    private val getChatCall: GetChatCall,
    private val getChatCallByCallIdUseCase: GetChatCallByCallIdUseCase,
    private val getChatCallIdsUseCase: GetChatCallIdsUseCase,
    private val answerChatCallUseCase: AnswerChatCallUseCase,
    private val holdChatCallUseCase: HoldChatCallUseCase,
    private val hangChatCallUseCase: HangChatCallUseCase,
    private val startChatCallNoRingingUseCase: StartChatCallNoRingingUseCase,
) {

    /**
     * Start or join scheduled meeting
     *
     * @param chatId            Chat room Id
     * @param schedId           Scheduled meeting id
     * @param enableVideo       Flag to check if video should be enabled
     * @param enableAudio       Flag to check if audio should be enabled
     * @return                  Chat call
     */
    suspend operator fun invoke(
        chatId: Long,
        schedId: Long,
        enableVideo: Boolean,
        enableAudio: Boolean,
    ): ChatCall? {
        val currentChatCall = getChatCall(chatId)
        val activeChatCalls = getActiveChatCalls(currentChatCall?.callId)

        return when {
            activeChatCalls.isEmpty() -> {
                currentChatCall.answerOrStart(chatId, schedId, enableAudio, enableVideo)
            }

            activeChatCalls.size == 1 -> {
                activeChatCalls.first().takeIf { !it.isOnHold }?.holdOrHangUp()

                currentChatCall.answerOrStart(chatId, schedId, enableAudio, enableVideo)
            }

            else -> {
                activeChatCalls.filter { !it.isOnHold }.forEach { hangChatCallUseCase(it.callId) }

                currentChatCall.answerOrStart(chatId, schedId, enableAudio, enableVideo)
            }
        }
    }

    private suspend fun getActiveChatCalls(currentChatCallId: Long?): List<ChatCall> =
        getChatCallIdsUseCase()
            .filter { it != currentChatCallId }
            .mapNotNull { callId ->
                getChatCallByCallIdUseCase(callId)?.takeIf { chatCall ->
                    chatCall.status == ChatCallStatus.Connecting
                            || chatCall.status == ChatCallStatus.InProgress
                            || chatCall.status == ChatCallStatus.Joining
                }
            }

    private suspend fun ChatCall?.answerOrStart(
        chatId: Long,
        schedId: Long,
        enableVideo: Boolean,
        enableAudio: Boolean,
    ): ChatCall? = when {
        this == null ->
            startChatCallNoRingingUseCase(chatId, schedId, enableVideo, enableAudio)

        status == ChatCallStatus.UserNoPresent ->
            answerChatCallUseCase(chatId, enableVideo, enableAudio)

        else ->
            this
    }

    private suspend fun ChatCall.holdOrHangUp() {
        runCatching {
            holdChatCallUseCase(chatId, true)
        }.onFailure {
            hangChatCallUseCase(callId)
        }
    }
}
