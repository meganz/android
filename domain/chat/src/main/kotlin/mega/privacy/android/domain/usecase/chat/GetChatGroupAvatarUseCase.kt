package mega.privacy.android.domain.usecase.chat

import mega.privacy.android.domain.entity.chat.ChatAvatarItem
import mega.privacy.android.domain.exception.ChatRoomDoesNotExistException
import mega.privacy.android.domain.repository.AccountRepository
import mega.privacy.android.domain.repository.AvatarRepository
import mega.privacy.android.domain.repository.ChatParticipantsRepository
import mega.privacy.android.domain.usecase.GetCombinedChatRoomUseCase
import mega.privacy.android.domain.usecase.contact.GetUserFirstName
import javax.inject.Inject

/**
 * Get chat group avatar use case
 *
 * @property chatParticipantsRepository
 * @property accountRepository
 * @property avatarRepository
 * @property getUserFirstName
 * @property getCombinedChatRoomUseCase
 */
class GetChatGroupAvatarUseCase @Inject constructor(
    private val chatParticipantsRepository: ChatParticipantsRepository,
    private val accountRepository: AccountRepository,
    private val avatarRepository: AvatarRepository,
    private val getUserFirstName: GetUserFirstName,
    private val getCombinedChatRoomUseCase: GetCombinedChatRoomUseCase,
) {

    /**
     * Retrieve avatars for a chat room id
     *
     * @param chatId    Chat Id
     * @return          List of [ChatAvatarItem]
     */
    suspend operator fun invoke(chatId: Long): List<ChatAvatarItem> {
        val avatars = mutableListOf<ChatAvatarItem>()
        val chatRoom = getCombinedChatRoomUseCase(chatId) ?: throw ChatRoomDoesNotExistException()
        val participants = chatParticipantsRepository.getChatParticipantsHandles(chatId, 2)
        val myAccount = accountRepository.getUserAccount()
        val myHandle = myAccount.userId?.id ?: -1

        when {
            !chatRoom.isActive || (chatRoom.isGroup && participants.isEmpty()) -> {
                avatars.add(
                    ChatAvatarItem(
                        placeholderText = chatRoom.title
                    )
                )
            }

            !chatRoom.isGroup -> {
                avatars.add(
                    ChatAvatarItem(
                        placeholderText = getUserFirstName(chatRoom.peerHandle) ?: chatRoom.title,
                        uri = getAvatarPath(chatRoom.peerHandle),
                        color = avatarRepository.getAvatarColor(chatRoom.peerHandle),
                    )
                )
            }

            participants.size == 1 -> {
                avatars.add(
                    ChatAvatarItem(
                        placeholderText = myAccount.fullName ?: chatRoom.title,
                        uri = avatarRepository.getMyAvatarFile()?.absolutePath,
                        color = avatarRepository.getAvatarColor(myHandle),
                    )
                )
                participants.getOrNull(0)?.let { userHandle ->
                    avatars.add(
                        ChatAvatarItem(
                            placeholderText = getUserFirstName(userHandle),
                            uri = getAvatarPath(userHandle),
                            color = avatarRepository.getAvatarColor(userHandle),
                        )
                    )
                }
            }

            else -> {
                participants.forEach { userHandle ->
                    avatars.add(
                        if (userHandle == myHandle) {
                            ChatAvatarItem(
                                placeholderText = myAccount.fullName ?: chatRoom.title,
                                uri = avatarRepository.getMyAvatarFile()?.absolutePath,
                                color = avatarRepository.getAvatarColor(myHandle),
                            )
                        } else {
                            ChatAvatarItem(
                                placeholderText = getUserFirstName(userHandle) ?: chatRoom.title,
                                uri = getAvatarPath(userHandle),
                                color = avatarRepository.getAvatarColor(userHandle),
                            )
                        }
                    )
                }
            }
        }

        return avatars
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
