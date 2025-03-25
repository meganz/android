package mega.privacy.android.domain.usecase.chat

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import mega.privacy.android.domain.repository.ChatParticipantsRepository
import javax.inject.Inject
import kotlin.time.Duration.Companion.seconds

/**
 * Set user typing status use case
 *
 * @property chatParticipantsRepository
 * @property scope
 */
class SetUserTypingStatusUseCase(
    private val chatParticipantsRepository: ChatParticipantsRepository,
    private val scope: CoroutineScope,
    private val getCurrentTimeInMilliSeconds: () -> Long,
) {

    @Inject
    constructor(
        chatParticipantsRepository: ChatParticipantsRepository,
    ) : this(chatParticipantsRepository, CoroutineScope(Dispatchers.IO), System::currentTimeMillis)

    private var typingJobPair: Pair<Long, Job>? = null
    private var acceptNextEventAfter: Long = 0

    /**
     * Invoke
     *
     * @param isUserTyping
     * @param chatId
     */
    suspend operator fun invoke(isUserTyping: Boolean, chatId: Long) {
        handleEvent(chatId, isUserTyping)
    }

    private suspend fun handleEvent(chatId: Long, isUserTyping: Boolean) {
        clearPreviousEventIfPresent(chatId)
        if (isUserTyping) {
            if (debounceTimeExpired()) handleTypingEvent(chatId)
        } else {
            chatParticipantsRepository.setUserStopTyping(chatId)
        }
    }

    private suspend fun clearPreviousEventIfPresent(chatId: Long) {
        typingJobPair?.let { (id, job) ->
            job.cancel()
            if (id != chatId) changeChats(id)
        }
        typingJobPair = null
    }

    private fun debounceTimeExpired(): Boolean {
        val currentTime = getCurrentTimeInMilliSeconds()
        if (currentTime >= acceptNextEventAfter) {
            acceptNextEventAfter = currentTime + 4.seconds.inWholeMilliseconds
            return true
        } else {
            return false
        }
    }

    private suspend fun handleTypingEvent(chatId: Long) {
        typingJobPair = Pair(
            chatId,
            scope.launch {
                delay(5.seconds)
                chatParticipantsRepository.setUserStopTyping(chatId)
            }
        )
        chatParticipantsRepository.setUserStartTyping(chatId)
    }

    private suspend fun changeChats(id: Long) {
        chatParticipantsRepository.setUserStopTyping(id)
        acceptNextEventAfter = 0
    }
}