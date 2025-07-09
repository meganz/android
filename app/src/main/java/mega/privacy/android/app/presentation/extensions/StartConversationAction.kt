package mega.privacy.android.app.presentation.extensions

import androidx.compose.ui.graphics.vector.ImageVector
import mega.privacy.android.app.R
import mega.privacy.android.app.presentation.startconversation.model.StartConversationAction
import mega.privacy.android.icon.pack.IconPack

internal val StartConversationAction.title: Int
    get() = when (this) {
        StartConversationAction.NewGroup -> R.string.new_group_chat_label
        StartConversationAction.NewMeeting -> R.string.new_meeting
        StartConversationAction.JoinMeeting -> R.string.join_meeting
    }

internal val StartConversationAction.icon: ImageVector
    get() = when (this) {
        StartConversationAction.NewGroup -> IconPack.Medium.Thin.Outline.MessageChatCircle
        StartConversationAction.NewMeeting -> IconPack.Medium.Thin.Outline.VideoPlus
        StartConversationAction.JoinMeeting -> IconPack.Medium.Thin.Outline.VideoJoin
    }