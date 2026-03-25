package mega.privacy.android.shared.chats.model

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import java.io.File
import mega.privacy.android.domain.entity.chat.ChatStatus
import mega.privacy.android.icon.pack.IconPack

/**
 * UI model for one row in the chat explorer list.
 *
 * Variants: [NoteToSelf], [GroupChat], [Meeting], [OneToOneChat], [Contact].
 *
 * Shared structure is grouped under [GroupChatAndMeeting] (group + meeting) and [OneToOneChatAndContact] (1:1 + contact).
 */
@Immutable
sealed class ChatExplorerUiItem {

    abstract val isSelected: Boolean
    abstract val isEnabled: Boolean
    open val icon: ImageVector? = null
    open val hasAvatarIcon: Boolean = false

    val avatarPrimaryColor: Color?
        get() = when (this) {
            is OneToOneChatAndContact -> primaryColor
            is NoteToSelf, is GroupChat -> Color(0xFF00ACC1)
            is Meeting -> Color(0xFF00897B)
        }

    val avatarSecondaryColor: Color?
        get() = when (this) {
            is OneToOneChatAndContact -> secondaryColor ?: primaryColor
            is NoteToSelf, is GroupChat -> Color(0xFF00BDB2)
            is Meeting -> Color(0xFF00ACC1)
        }

    /**
     * Note-to-self row (hint or not hint).
     */
    @Immutable
    data class NoteToSelf(
        val isHint: Boolean,
        override val isSelected: Boolean,
        override val isEnabled: Boolean = true,
        override val icon: ImageVector = IconPack.Medium.Thin.Outline.FileText,
        override val hasAvatarIcon: Boolean = !isHint,
    ) : ChatExplorerUiItem()

    /**
     * Group or meeting row: shared title + participant count for subtitles.
     */
    @Immutable
    sealed class GroupChatAndMeeting : ChatExplorerUiItem() {
        abstract val participants: Int?
        abstract val title: String?
    }

    /**
     * Meeting row.
     */
    @Immutable
    data class Meeting(
        override val isSelected: Boolean,
        override val isEnabled: Boolean,
        override val participants: Int,
        override val title: String,
        override val icon: ImageVector = IconPack.Medium.Thin.Solid.Video,
        override val hasAvatarIcon: Boolean = true,
    ) : GroupChatAndMeeting()

    /**
     * Group chat row.
     */
    @Immutable
    data class GroupChat(
        override val isSelected: Boolean,
        override val isEnabled: Boolean,
        override val participants: Int,
        override val title: String,
        override val icon: ImageVector = IconPack.Medium.Thin.Solid.MessageChatCircle,
        override val hasAvatarIcon: Boolean = true,
    ) : GroupChatAndMeeting()

    /**
     * One-to-one chat or contact row: name, email, user status, avatar colors, optional local avatar file.
     */
    @Immutable
    sealed class OneToOneChatAndContact : ChatExplorerUiItem() {
        abstract val contactName: String?
        abstract val contactEmail: String?
        abstract val contactAvatarFile: File?
        abstract val primaryColor: Color
        abstract val secondaryColor: Color?
        abstract val userStatus: ChatStatus?
    }

    /**
     * Existing 1:1 chat room row.
     */
    @Immutable
    data class OneToOneChat(
        override val contactName: String?,
        override val contactEmail: String? = null,
        override val contactAvatarFile: File? = null,
        override val primaryColor: Color,
        override val secondaryColor: Color? = null,
        override val userStatus: ChatStatus,
        override val isSelected: Boolean,
        override val isEnabled: Boolean,
    ) : OneToOneChatAndContact()

    /**
     * Contact without an open chat (e.g. pick to start a chat).
     */
    @Immutable
    data class Contact(
        override val contactName: String?,
        override val userStatus: ChatStatus,
        override val contactEmail: String? = null,
        override val contactAvatarFile: File? = null,
        override val primaryColor: Color,
        override val secondaryColor: Color? = null,
        override val isSelected: Boolean,
        override val isEnabled: Boolean,
    ) : OneToOneChatAndContact()
}
