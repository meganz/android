package mega.privacy.android.domain.usecase.meeting

import mega.privacy.android.domain.entity.ChatRequest
import mega.privacy.android.domain.repository.CallRepository
import javax.inject.Inject

/**
 * Allow a list of users in the waiting room to join the call.
 */
class AllowUsersJoinCallUseCase @Inject constructor(
    private val callRepository: CallRepository,
) {

    /**
     * Invoke
     *
     * @param chatId Chat id
     * @param userList List of users that must be pushed into waiting room.
     * @param all if true indicates that all users with non moderator role, must be pushed into waiting room
     * @return                  [ChatRequest]
     */
    suspend operator fun invoke(
        chatId: Long,
        userList: List<Long>,
        all: Boolean,
    ): ChatRequest = callRepository.allowUsersJoinCall(
        chatId,
        userList,
        all,
    )
}