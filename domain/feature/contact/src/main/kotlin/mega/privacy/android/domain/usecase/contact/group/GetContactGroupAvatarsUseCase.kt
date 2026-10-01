package mega.privacy.android.domain.usecase.contact.group

import mega.privacy.android.domain.entity.chat.ChatAvatarItem
import mega.privacy.android.domain.entity.chat.ChatRoom
import mega.privacy.android.domain.extension.mapAsync
import mega.privacy.android.domain.repository.AccountRepository
import mega.privacy.android.domain.repository.AvatarRepository
import mega.privacy.android.domain.repository.ChatParticipantsRepository
import mega.privacy.android.domain.usecase.contact.GetUserFirstName
import javax.inject.Inject

/**
 * Get contact group avatars use case.
 *
 * Loads the avatar items for a collection of group chat rooms in a single batch. The current
 * user account is fetched only once and each unique participant is resolved only once, regardless
 * of how many groups they belong to, avoiding the duplicated work of resolving the same user per
 * group.
 *
 * @property chatParticipantsRepository
 * @property accountRepository
 * @property avatarRepository
 * @property getUserFirstName
 */
class GetContactGroupAvatarsUseCase @Inject constructor(
    private val chatParticipantsRepository: ChatParticipantsRepository,
    private val accountRepository: AccountRepository,
    private val avatarRepository: AvatarRepository,
    private val getUserFirstName: GetUserFirstName,
) {

    /**
     * Invoke.
     *
     * @param chatRooms Group chat rooms to load avatars for.
     * @return Map of chat id to its list of [ChatAvatarItem]s.
     */
    suspend operator fun invoke(chatRooms: List<ChatRoom>): Map<Long, List<ChatAvatarItem>> {
        val myAccount = accountRepository.getUserAccount()
        val myHandle = myAccount.userId?.id ?: INVALID_HANDLE

        val participantsByChat = chatRooms
            .mapAsync { room ->
                room.chatId to chatParticipantsRepository.getChatParticipantsHandles(
                    chatId = room.chatId,
                    limit = MAX_AVATARS,
                )
            }
            .toMap()

        val userAvatars = participantsByChat.values.flatten().toSet()
            .mapAsync { handle -> handle to resolveUser(handle) }
            .toMap()

        val self = ResolvedAvatar(
            placeholderText = myAccount.fullName,
            uri = avatarRepository.getMyAvatarFile()?.absolutePath,
            color = avatarRepository.getAvatarColor(myHandle),
        )

        return chatRooms.associate { room ->
            room.chatId to buildAvatars(
                room = room,
                participants = participantsByChat[room.chatId].orEmpty(),
                myHandle = myHandle,
                self = self,
                userAvatars = userAvatars,
            )
        }
    }

    private fun buildAvatars(
        room: ChatRoom,
        participants: List<Long>,
        myHandle: Long,
        self: ResolvedAvatar,
        userAvatars: Map<Long, ResolvedAvatar>,
    ): List<ChatAvatarItem> = when {
        !room.isActive || participants.isEmpty() ->
            listOf(ChatAvatarItem(placeholderText = room.title))

        participants.size == 1 -> listOf(
            self.toAvatarItem(room.title),
            userAvatars[participants.first()].toAvatarItem(room.title),
        )

        else -> participants.map { handle ->
            if (handle == myHandle) {
                self.toAvatarItem(room.title)
            } else {
                userAvatars[handle].toAvatarItem(room.title)
            }
        }
    }

    private suspend fun resolveUser(handle: Long) = ResolvedAvatar(
        placeholderText = runCatching {
            getUserFirstName(handle, skipCache = false, shouldNotify = false)
        }.getOrNull(),
        uri = runCatching { avatarRepository.getAvatarFile(handle) }.getOrNull()?.absolutePath,
        color = avatarRepository.getAvatarColor(handle),
    )

    private fun ResolvedAvatar?.toAvatarItem(fallbackTitle: String) = ChatAvatarItem(
        placeholderText = this?.placeholderText ?: fallbackTitle,
        uri = this?.uri,
        color = this?.color,
    )

    private data class ResolvedAvatar(
        val placeholderText: String?,
        val uri: String?,
        val color: Int?,
    )

    private companion object {
        const val INVALID_HANDLE = -1L
        const val MAX_AVATARS = 2
    }
}
