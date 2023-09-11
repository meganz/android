package mega.privacy.android.domain.usecase.chat

import mega.privacy.android.domain.repository.ChatRepository
import javax.inject.Inject

/**
 * Use Case to create ephemeral account plus plus
 *
 * @property chatRepository [ChatRepository]
 */
class CreateEphemeralAccountUseCase @Inject constructor(
    private val chatRepository: ChatRepository,
) {

    /**
     * Invoke
     *
     * @param firstName     User first name
     * @param lastName      User last name
     * @return              Session id to resume the process
     */
    suspend operator fun invoke(firstName: String, lastName: String): String =
        chatRepository.createEphemeralAccountPlusPlus(firstName, lastName)
}
