package mega.privacy.android.feature.cloudexplorer.presentation.chatexplorer

import androidx.compose.ui.graphics.Color
import mega.privacy.android.domain.entity.ChatRoomPermission
import mega.privacy.android.domain.entity.chat.ChatListItem
import mega.privacy.android.domain.entity.chat.ChatStatus
import mega.privacy.android.domain.entity.contacts.UserChatStatus
import mega.privacy.android.domain.entity.contacts.UserContact
import mega.privacy.android.domain.usecase.GetCombinedChatRoomUseCase
import mega.privacy.android.domain.usecase.avatar.GetUserAvatarColorUseCase
import mega.privacy.android.domain.usecase.avatar.GetUserAvatarSecondaryColorUseCase
import mega.privacy.android.domain.usecase.contact.GetUserOnlineStatusByHandleUseCase
import mega.privacy.android.shared.chats.model.ChatExplorerUiItem
import timber.log.Timber
import javax.inject.Inject

internal class ChatExplorerUiItemMapper @Inject constructor(
    private val getUserAvatarColorUseCase: GetUserAvatarColorUseCase,
    private val getUserAvatarSecondaryColorUseCase: GetUserAvatarSecondaryColorUseCase,
    private val getUserOnlineStatusByHandleUseCase: GetUserOnlineStatusByHandleUseCase,
    private val getCombinedChatRoomUseCase: GetCombinedChatRoomUseCase,
) {

    suspend operator fun invoke(chat: ChatListItem): ChatExplorerUiItem {
        val isWritable = chat.ownPrivilege >= ChatRoomPermission.Standard

        return when {
            chat.isNoteToSelf -> ChatExplorerUiItem.NoteToSelf(
                id = chat.chatId,
                isHint = false,
                isSelected = false,
                isEnabled = isWritable,
                isArchived = chat.isArchived,
                lastTimestamp = chat.lastTimestamp,
            )

            chat.isGroup || chat.peerHandle == INVALID_HANDLE ->
                groupOrMeetingUiItem(chat, isWritable)

            else -> ChatExplorerUiItem.OneToOneChat(
                id = chat.chatId,
                contactName = chat.title,
                primaryColor = userAvatarColor(chat.peerHandle),
                secondaryColor = userAvatarSecondaryColor(chat.peerHandle),
                userStatus = onlineStatus(chat.peerHandle),
                isSelected = false,
                isEnabled = isWritable,
                isArchived = chat.isArchived,
                lastTimestamp = chat.lastTimestamp,
            )
        }
    }

    suspend operator fun invoke(contact: UserContact): ChatExplorerUiItem? {
        val user = contact.user ?: return null
        val displayName = contact.contact?.fullName ?: user.email

        return ChatExplorerUiItem.Contact(
            id = user.handle,
            contactName = displayName,
            contactEmail = user.email,
            primaryColor = userAvatarColor(user.handle),
            secondaryColor = userAvatarSecondaryColor(user.handle),
            userStatus = onlineStatus(user.handle),
            isSelected = false,
            isEnabled = true,
        )
    }

    private suspend fun groupOrMeetingUiItem(
        chat: ChatListItem,
        isWritable: Boolean,
    ): ChatExplorerUiItem {
        val combined = runCatching { getCombinedChatRoomUseCase(chat.chatId) }
            .onFailure {
                Timber.e(
                    it,
                    "Failed to load combined chat room for chatId=${chat.chatId}"
                )
            }
            .getOrNull()
        val participants = combined?.peerCount?.toInt() ?: 0

        return if (combined?.isMeeting == true) {
            ChatExplorerUiItem.Meeting(
                id = chat.chatId,
                title = chat.title,
                participants = participants,
                isSelected = false,
                isEnabled = isWritable,
                isArchived = chat.isArchived,
                lastTimestamp = chat.lastTimestamp,
            )
        } else {
            ChatExplorerUiItem.GroupChat(
                id = chat.chatId,
                title = chat.title,
                participants = participants,
                isSelected = false,
                isEnabled = isWritable,
                isArchived = chat.isArchived,
                lastTimestamp = chat.lastTimestamp,
            )
        }
    }

    private suspend fun userAvatarColor(userHandle: Long): Color =
        runCatching { getUserAvatarColorUseCase(userHandle) }
            .onFailure { Timber.e(it, "Failed to load avatar color for handle=$userHandle") }
            .map { Color(it) }
            .getOrDefault(Color.Unspecified)

    private suspend fun userAvatarSecondaryColor(userHandle: Long): Color? =
        runCatching { getUserAvatarSecondaryColorUseCase(userHandle) }
            .onFailure {
                Timber.e(
                    it,
                    "Failed to load secondary avatar color for handle=$userHandle"
                )
            }
            .map { Color(it) }
            .getOrNull()

    private suspend fun onlineStatus(userHandle: Long): ChatStatus =
        runCatching { getUserOnlineStatusByHandleUseCase(userHandle) }
            .onFailure { Timber.e(it, "Failed to load online status for handle=$userHandle") }
            .getOrNull()
            ?.toChatStatus()
            ?: ChatStatus.Offline

    private fun UserChatStatus.toChatStatus(): ChatStatus = when (this) {
        UserChatStatus.Online -> ChatStatus.Online
        UserChatStatus.Away -> ChatStatus.Away
        UserChatStatus.Busy -> ChatStatus.Busy
        UserChatStatus.Offline -> ChatStatus.Offline
        UserChatStatus.Invalid -> ChatStatus.Offline
    }

    private companion object {
        private const val INVALID_HANDLE = -1L
    }
}
