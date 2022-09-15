package mega.privacy.android.app.presentation.extensions

import mega.privacy.android.app.R
import mega.privacy.android.app.presentation.startconversation.model.StartConversationAction

internal val StartConversationAction.title: Int
    get() = when (this) {
        StartConversationAction.NewGroup -> R.string.new_group_chat_label
        StartConversationAction.NewMeeting -> R.string.new_meeting
        StartConversationAction.JoinMeeting -> R.string.join_meeting
    }

internal val StartConversationAction.icon: Int
    get() = when (this) {
        StartConversationAction.NewGroup -> R.drawable.ic_new_group
        StartConversationAction.NewMeeting -> R.drawable.ic_new_meeting
        StartConversationAction.JoinMeeting -> R.drawable.ic_join_meeting
    }