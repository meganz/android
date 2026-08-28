package mega.privacy.android.domain.usecase.chat

import mega.privacy.android.domain.repository.ChatRepository
import javax.inject.Inject

/**
 * Set presence persist use case
 *
 * @property chatRepository
 */
class SetPresencePersistUseCase @Inject constructor(
    private val chatRepository: ChatRepository,
) {

    /**
     * Invoke
     *
     * @param enabled True to persist the online status even when disconnected
     */
    suspend operator fun invoke(enabled: Boolean) =
        chatRepository.setPresencePersist(enabled)
}
