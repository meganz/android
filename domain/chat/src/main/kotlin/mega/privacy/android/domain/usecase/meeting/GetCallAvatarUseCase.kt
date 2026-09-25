package mega.privacy.android.domain.usecase.meeting

import mega.privacy.android.domain.entity.chat.ChatAvatarItem
import mega.privacy.android.domain.exception.ChatRoomDoesNotExistException
import mega.privacy.android.domain.repository.AvatarRepository
import mega.privacy.android.domain.usecase.GetChatRoomUseCase
import mega.privacy.android.domain.usecase.contact.GetUserFirstName
import javax.inject.Inject

/**
 * Get chat group avatar use case
 *
 * @property avatarRepository
 * @property getUserFirstName
 */
class GetCallAvatarUseCase @Inject constructor(
    private val getChatRoomUseCase: GetChatRoomUseCase,
    private val avatarRepository: AvatarRepository,
    private val getUserFirstName: GetUserFirstName,
) {
    /**
     * Retrieve avatars for a chat room id
     *
     * @param callerHandle        User Handle
     * @return                  ChatAvatarItem
     */
    suspend operator fun invoke(chatId: Long, callerHandle: Long): ChatAvatarItem {
        val chatRoom = getChatRoomUseCase(chatId) ?: throw ChatRoomDoesNotExistException()

        val isOneToOneChat = !chatRoom.isGroup && !chatRoom.isMeeting
        return when {
            isOneToOneChat ->
                ChatAvatarItem(
                    placeholderText = getUserFirstName(callerHandle),
                    uri = getAvatarPath(callerHandle),
                    color = avatarRepository.getAvatarColor(callerHandle)
                )

            else ->
                ChatAvatarItem(
                    placeholderText = chatRoom.title
                )
        }
    }

    private suspend fun getUserFirstName(userHandle: Long): String? =
        runCatching {
            getUserFirstName(userHandle, skipCache = false, shouldNotify = false)
        }.getOrNull()

    private suspend fun getAvatarPath(userHandle: Long): String? =
        runCatching {
            avatarRepository.getAvatarFile(userHandle)
        }.getOrNull()?.absolutePath
}