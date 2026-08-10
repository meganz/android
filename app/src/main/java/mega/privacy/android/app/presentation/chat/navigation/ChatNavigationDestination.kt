package mega.privacy.android.app.presentation.chat.navigation

import android.content.Context
import android.content.Intent
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalContext
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import mega.privacy.android.app.presentation.meeting.chat.ChatActivity
import mega.privacy.android.app.presentation.meeting.chat.model.EXTRA_ACTION
import mega.privacy.android.app.presentation.meeting.chat.model.EXTRA_LINK
import mega.privacy.android.app.utils.Constants
import mega.privacy.android.navigation.contract.NavigationHandler
import mega.privacy.android.navigation.contract.transparent.transparentMetadata
import mega.privacy.android.navigation.destination.ChatNavKey
import mega.privacy.android.navigation.destination.ShowChatMessagesNavKey

fun EntryProviderScope<NavKey>.chatLegacyDestination(removeDestination: () -> Unit) {
    entry<ChatNavKey>(
        metadata = transparentMetadata()
    ) { key ->
        val context = LocalContext.current
        LaunchedEffect(Unit) {
            context.startActivity(createChatIntent(context, key))
            removeDestination()
        }
    }
}

/**
 * Destination for [ShowChatMessagesNavKey]. Opens [ChatActivity] for the given chat with
 * the "show messages" action hardcoded.
 * @param navigationHandler
 */
fun EntryProviderScope<NavKey>.showChatMessagesDestination(navigationHandler: NavigationHandler) {
    entry<ShowChatMessagesNavKey>(
        metadata = transparentMetadata()
    ) { key ->
        val context = LocalContext.current
        LaunchedEffect(Unit) {
            context.startActivity(createShowMessagesIntent(context, key.chatId))
            navigationHandler.remove(key)
        }
    }
}

@Suppress("DEPRECATION") // The activity is launched via the legacy intent action.
private fun createShowMessagesIntent(context: Context, chatId: Long): Intent =
    Intent(context, ChatActivity::class.java).apply {
        action = Constants.ACTION_CHAT_SHOW_MESSAGES
        putExtra(ChatNavKey.LEGACY_CHAT_ID, chatId)
        putExtra(EXTRA_ACTION, Constants.ACTION_CHAT_SHOW_MESSAGES)
    }

/**
 * Creates an Intent for ChatHostActivity with all relevant extras
 *
 * @param context Context to create the Intent
 * @param key ChatNavKey containing all navigation parameters
 * @return Intent configured for ChatHostActivity
 */
private fun createChatIntent(context: Context, key: ChatNavKey): Intent {
    return Intent(context, ChatActivity::class.java).apply {
        // Set action if provided
        key.action?.let { action = it }

        // Specific chat extras
        putExtra(ChatNavKey.LEGACY_CHAT_ID, key.chatId)
        key.action?.let { putExtra(EXTRA_ACTION, it) }
        key.link?.let { putExtra(EXTRA_LINK, it) }
        key.snackbarText?.let { putExtra(Constants.SHOW_SNACKBAR, it) }
        key.messageId?.let { putExtra(Constants.ID_MSG, it) }
        key.isOverQuota?.let { putExtra(Constants.IS_OVER_QUOTA, it) }
    }

}

