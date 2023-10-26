package mega.privacy.android.domain.usecase.chat

import mega.privacy.android.domain.repository.ContactsRepository
import javax.inject.Inject

/**
 * Use case for get chat participant email
 */
class GetChatParticipantEmailUseCase @Inject constructor(
    private val contactsRepository: ContactsRepository,
) {

    /**
     * Invoke
     *
     * @return  Email
     */
    suspend operator fun invoke(handle: Long): String? {
        return runCatching {
            contactsRepository.getUserEmail(handle, false)
        }.fold(
            onSuccess = { request -> return request },
            onFailure = { null }
        )
    }
}