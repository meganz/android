package mega.privacy.android.domain.usecase.chat

import mega.privacy.android.domain.entity.ChatRoomPermission
import mega.privacy.android.domain.repository.ChatRepository
import javax.inject.Inject

/**
 * Get user privilege
 */
class GetUserPrivilegeUseCase @Inject constructor(
    private val chatRepository: ChatRepository,
) {
    suspend operator fun invoke(chatId: Long, userHandle: Long): ChatRoomPermission =
        chatRepository.getUserPrivilege(chatId, userHandle)
}