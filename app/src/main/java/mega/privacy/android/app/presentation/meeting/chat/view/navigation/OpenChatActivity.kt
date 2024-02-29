package mega.privacy.android.app.presentation.meeting.chat.view.navigation

import android.content.Context
import android.os.Bundle
import androidx.fragment.app.commit
import mega.privacy.android.app.presentation.meeting.chat.ChatFragment
import mega.privacy.android.app.presentation.meeting.chat.extension.findChatHostActivity
import mega.privacy.android.app.presentation.meeting.chat.model.EXTRA_ACTION
import mega.privacy.android.app.presentation.meeting.chat.model.EXTRA_LINK
import mega.privacy.android.app.utils.Constants
import mega.privacy.android.app.utils.Constants.ACTION_CHAT_SHOW_MESSAGES
import timber.log.Timber

/**
 * Open a new chat fragment in the current ChatHostActivity
 *
 * @param context It should be ChatHostActivity or a wrapper of it, if not the navigation won't happen
 * @param chatId The chat Id we want to open
 * @param chatLink optional chat link if the chat is opened from a link
 */
internal fun openChatFragment(
    context: Context,
    chatId: Long,
    chatLink: String? = null,
) {
    context.findChatHostActivity()?.apply {
        supportFragmentManager.commit {
            val extras = Bundle().apply {
                putString(EXTRA_ACTION, ACTION_CHAT_SHOW_MESSAGES)
                putLong(Constants.CHAT_ID, chatId)
                chatLink?.let {
                    putString(EXTRA_LINK, it)
                }
            }
            replace(android.R.id.content, ChatFragment::class.java, extras)
        }
    } ?: run {
        Timber.e("This navigation needs to be used from ChatHostActivity only")
    }
}