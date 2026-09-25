package mega.privacy.android.domain.usecase.chat

import mega.privacy.android.domain.repository.ChatRepository
import javax.inject.Inject

/**
 * Set last green visible use case
 *
 * @property chatRepository
 */
class SetLastGreenVisibleUseCase @Inject constructor(
    private val chatRepository: ChatRepository,
) {

    /**
     * Invoke
     *
     * @param enabled True to make the last time online visible to other users
     */
    suspend operator fun invoke(enabled: Boolean) =
        chatRepository.setLastGreenVisible(enabled)
}
