package mega.privacy.android.app.presentation.chat.navigation

import android.content.Context
import android.content.Intent
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalContext
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import mega.privacy.android.app.presentation.meeting.chat.ChatActivity
import mega.privacy.android.app.presentation.meeting.chat.model.EXTRA_ACTION
import mega.privacy.android.app.presentation.meeting.chat.model.EXTRA_LINK
import mega.privacy.android.app.utils.Constants
import mega.privacy.android.domain.featuretoggle.ApiFeatures
import mega.privacy.android.feature.chat.navigation.ChatRoomEntry
import mega.privacy.android.navigation.contract.NavigationHandler
import mega.privacy.android.navigation.contract.featureflag.FeatureFlagGate
import mega.privacy.android.navigation.destination.ChatNavKey
import mega.privacy.android.navigation.destination.ShowChatMessagesNavKey

/**
 * Destination for [ChatNavKey]. Behind [ApiFeatures.SingleActivityChatRoom] either renders the
 * Compose [ChatRoomEntry] inline (flag on) or launches the legacy [ChatActivity] and pops the
 * entry (flag off).
 *
 * The entry is not transparent for the same reason as the chat list destination: with the flag on
 * it is a screen of its own, and the flag is only known once the entry is composed, after its
 * metadata has been fixed.
 *
 * @param removeDestination
 */
fun EntryProviderScope<NavKey>.chatLegacyDestination(removeDestination: () -> Unit) {
    entry<ChatNavKey> { key ->
        FeatureFlagGate(
            feature = ApiFeatures.SingleActivityChatRoom,
            disabled = {
                LegacyChatEntry(
                    createIntent = { context -> createChatIntent(context, key) },
                    removeDestination = removeDestination,
                )
            },
            enabled = {
                ChatRoomEntry(
                    chatId = key.chatId,
                    action = key.action,
                    link = key.link,
                    openFromList = false,
                    onBack = removeDestination,
                )
            },
        )
    }
}

/**
 * Destination for [ShowChatMessagesNavKey]. Behind [ApiFeatures.SingleActivityChatRoom] either
 * renders the Compose [ChatRoomEntry] inline (flag on) or opens [ChatActivity] for the given chat
 * with the "show messages" action hardcoded and pops the entry (flag off).
 *
 * The entry is not transparent for the same reason as [chatLegacyDestination].
 *
 * @param navigationHandler
 */
fun EntryProviderScope<NavKey>.showChatMessagesDestination(navigationHandler: NavigationHandler) {
    entry<ShowChatMessagesNavKey> { key ->
        FeatureFlagGate(
            feature = ApiFeatures.SingleActivityChatRoom,
            disabled = {
                LegacyChatEntry(
                    createIntent = { context ->
                        createShowMessagesIntent(context, key.chatId, key.openFromList)
                    },
                    removeDestination = { navigationHandler.remove(key) },
                )
            },
            enabled = {
                ChatRoomEntry(
                    chatId = key.chatId,
                    action = Constants.ACTION_CHAT_SHOW_MESSAGES,
                    link = null,
                    openFromList = key.openFromList,
                    onBack = navigationHandler::back,
                )
            },
        )
    }
}

/**
 * Launches the legacy [ChatActivity] and pops the entry.
 */
@Composable
private fun LegacyChatEntry(
    createIntent: (Context) -> Intent,
    removeDestination: () -> Unit,
) {
    val context = LocalContext.current
    LaunchedEffect(Unit) {
        context.startActivity(createIntent(context))
        removeDestination()
    }
}

@Suppress("DEPRECATION") // The activity is launched via the legacy intent action.
private fun createShowMessagesIntent(context: Context, chatId: Long, openFromList: Boolean): Intent =
    Intent(context, ChatActivity::class.java).apply {
        action = Constants.ACTION_CHAT_SHOW_MESSAGES
        putExtra(ChatNavKey.LEGACY_CHAT_ID, chatId)
        putExtra(EXTRA_ACTION, Constants.ACTION_CHAT_SHOW_MESSAGES)
        putExtra(ChatActivity.EXTRA_OPEN_FROM_LIST, openFromList)
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
