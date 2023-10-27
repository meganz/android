package mega.privacy.android.domain.usecase.contact

import mega.privacy.android.domain.repository.ChatRepository
import javax.inject.Inject

/**
 * Get participant first name use case
 *
 */
class GetParticipantFirstNameUseCase @Inject constructor(
    private val contactsRepository: ChatRepository,
) {
    /**
     * Invoke.
     *
     * @param handle    User handle.
     * @return          First name.
     */
    suspend operator fun invoke(handle: Long): String? =
        contactsRepository.getParticipantFirstName(handle)
}