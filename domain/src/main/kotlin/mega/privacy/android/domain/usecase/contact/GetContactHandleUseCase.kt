package mega.privacy.android.domain.usecase.contact

import mega.privacy.android.domain.repository.ChatRepository
import javax.inject.Inject

/**
 * Use case to get the contact's handle by email
 *
 * @property chatRepository [ChatRepository]
 */
class GetContactHandleUseCase @Inject constructor(
    private val chatRepository: ChatRepository
) {

    /**
     * Invocation method
     *
     * @param email Contact's email
     * @return The contact's handle
     */
    suspend operator fun invoke(email: String): Long? =
        chatRepository.getContactHandle(email)
}
