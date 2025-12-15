package mega.privacy.android.app.presentation.meeting.chat.extension

import android.content.Context
import android.content.ContextWrapper
import mega.privacy.android.app.presentation.meeting.chat.ChatActivity

/**
 * Find the Chat Activity in a given Context.
 */
internal fun Context.findChatHostActivity(): ChatActivity? = when (this) {
    is ChatActivity -> this
    is ContextWrapper -> baseContext.findChatHostActivity()
    else -> null
}