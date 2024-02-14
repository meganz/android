package mega.privacy.android.app.presentation.meeting.chat.extension

import android.content.Context
import android.content.ContextWrapper
import mega.privacy.android.app.presentation.meeting.chat.ChatHostActivity

/**
 * Find the Chat Activity in a given Context.
 */
internal fun Context.findChatHostActivity(): ChatHostActivity? = when (this) {
    is ChatHostActivity -> this
    is ContextWrapper -> baseContext.findChatHostActivity()
    else -> null
}