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
import mega.privacy.android.navigation.contract.transparent.transparentMetadata
import mega.privacy.android.navigation.destination.ChatNavKey

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
        putExtra(Constants.CHAT_ID, key.chatId)
        key.action?.let { putExtra(EXTRA_ACTION, it) }
        key.link?.let { putExtra(EXTRA_LINK, it) }
        key.snackbarText?.let { putExtra(Constants.SHOW_SNACKBAR, it) }
        key.messageId?.let { putExtra(Constants.ID_MSG, it) }
        key.isOverQuota?.let { putExtra(Constants.IS_OVERQUOTA, it) }
    }

}

