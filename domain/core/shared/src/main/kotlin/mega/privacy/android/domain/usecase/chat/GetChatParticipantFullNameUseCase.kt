package mega.privacy.android.domain.usecase.chat

import mega.privacy.android.domain.repository.ContactsRepository
import javax.inject.Inject

/**
 * Use case for get chat participant full name
 */
class GetChatParticipantFullNameUseCase @Inject constructor(
    private val contactsRepository: ContactsRepository,
) {

    /**
     * Invoke
     *
     * @return  Full name
     */
    suspend operator fun invoke(handle: Long): String? {
        return runCatching {
            contactsRepository.getUserFullName(handle, false)
        }.fold(
            onSuccess = { request -> return request },
            onFailure = { null }
        )
    }
}