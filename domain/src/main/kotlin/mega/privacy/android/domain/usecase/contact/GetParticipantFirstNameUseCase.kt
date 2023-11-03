package mega.privacy.android.domain.usecase.contact

import mega.privacy.android.domain.repository.ChatRepository
import javax.inject.Inject

/**
 * Get participant first name use case
 *
 */
class GetParticipantFirstNameUseCase @Inject constructor(
    private val chatRepositoryRepository: ChatRepository,
) {
    /**
     * Invoke.
     *
     * @param handle    User handle.
     * @param contemplateEmail True if should return the email if the name is not found, false otherwise.
     * @return          First name.
     */
    suspend operator fun invoke(handle: Long, contemplateEmail: Boolean = true): String? =
        chatRepositoryRepository.getParticipantFirstName(handle, contemplateEmail)
}