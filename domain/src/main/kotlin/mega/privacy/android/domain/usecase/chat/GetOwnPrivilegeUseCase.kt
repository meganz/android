package mega.privacy.android.domain.usecase.chat

import mega.privacy.android.domain.entity.ChatRoomPermission
import mega.privacy.android.domain.repository.ChatRepository
import javax.inject.Inject


/**
 * Get own privilege
 */
class GetOwnPrivilegeUseCase @Inject constructor(
    private val chatRepository: ChatRepository,
) {
    suspend operator fun invoke(chatId: Long): ChatRoomPermission =
        chatRepository.getOwnPrivilege(chatId)
}