package mega.privacy.android.domain.usecase.contact

import mega.privacy.android.domain.repository.ChatRepository
import javax.inject.Inject

/**
 * Get participant full name use case
 *
 * @property chatRepositoryRepository
 */
class GetParticipantFullNameUseCase @Inject constructor(
    private val chatRepositoryRepository: ChatRepository,
) {
    /**
     * Invoke.
     *
     * @param handle    User handle.
     * @return          First name.
     */
    suspend operator fun invoke(handle: Long): String? =
        chatRepositoryRepository.getParticipantFullName(handle)
}