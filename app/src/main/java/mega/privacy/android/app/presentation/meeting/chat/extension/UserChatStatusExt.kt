package mega.privacy.android.app.presentation.meeting.chat.extension

import mega.privacy.android.core.ui.controls.chat.UiChatStatus
import mega.privacy.android.domain.entity.contacts.UserChatStatus

/**
 * To chat status
 *
 * @return Chat status
 */
fun UserChatStatus?.toUiChatStatus(): UiChatStatus? = when (this) {
    UserChatStatus.Online -> UiChatStatus.Online
    UserChatStatus.Away -> UiChatStatus.Away
    UserChatStatus.Busy -> UiChatStatus.Busy
    UserChatStatus.Offline -> UiChatStatus.Offline
    else -> null
}
