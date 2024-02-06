package mega.privacy.android.domain.usecase.chat.message

import mega.privacy.android.domain.repository.ChatRepository
import mega.privacy.android.domain.repository.chat.ChatMessageRepository
import javax.inject.Inject

/**
 * Use case for attaching one or more contacts to a chat.
 */
class AttachContactsUseCase @Inject constructor(
    private val chatRepository: ChatRepository,
    private val chatMessageRepository: ChatMessageRepository,
    private val createSaveSentMessageRequestUseCase: CreateSaveSentMessageRequestUseCase,
) {

    /**
     * Invoke.
     *
     * @param chatId Chat id.
     * @param contactEmails List of contact emails.
     */
    suspend operator fun invoke(chatId: Long, contactEmails: List<String>) {
        val requests = buildList {
            contactEmails.forEach { email ->
                chatMessageRepository.attachContact(chatId, email)?.let {
                    val request = createSaveSentMessageRequestUseCase(it)
                    add(request)
                }
            }
        }
        if (requests.isNotEmpty()) {
            chatRepository.storeMessages(chatId, requests)
        }
    }
}