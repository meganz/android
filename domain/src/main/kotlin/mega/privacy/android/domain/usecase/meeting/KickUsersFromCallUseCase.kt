package mega.privacy.android.domain.usecase.meeting

import mega.privacy.android.domain.entity.ChatRequest
import mega.privacy.android.domain.repository.CallRepository
import javax.inject.Inject

/**
 * Disconnects all clients of the specified users, regardless of whether they are in the call or in the waiting room.
 */
class KickUsersFromCallUseCase @Inject constructor(
    private val callRepository: CallRepository,
) {

    /**
     * Invoke
     *
     * @param chatId Chat id
     * @param userList List of users that must be pushed into waiting room.
     * @return                  [ChatRequest]
     */
    suspend operator fun invoke(
        chatId: Long,
        userList: List<Long>,
    ): ChatRequest = callRepository.kickUsersFromCall(
        chatId,
        userList,
    )
}