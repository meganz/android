package mega.privacy.android.app.presentation.chat.navigation

import android.content.Intent
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalContext
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import mega.privacy.android.app.presentation.meeting.chat.ChatActivity
import mega.privacy.android.domain.featuretoggle.ApiFeatures
import mega.privacy.android.feature.chat.navigation.ChatListEntry
import mega.privacy.android.navigation.contract.NavigationHandler
import mega.privacy.android.navigation.contract.featureflag.FeatureFlagGate
import mega.privacy.android.navigation.destination.ChatListNavKey

/**
 * Navigation destination for the chat list. Behind
 * [ApiFeatures.CustomisableBottomNavigation] either renders the Compose
 * [ChatListEntry] inline (flag on) or launches the legacy [ChatActivity]
 * chat list and pops the entry (flag off).
 *
 * The entry is deliberately not transparent. A transparent entry renders as an overlay above the
 * scene beneath it, which only suits entries that show nothing while they hand off to an activity.
 * The chat list is a screen of its own, and an overlay entry is composed twice while a pop onto it
 * settles - once as the scene the pop animates towards and once as the overlay - which the
 * saveable state holder rejects.
 */
fun EntryProviderScope<NavKey>.chatListDestination(navigationHandler: NavigationHandler) {
    entry<ChatListNavKey> { key ->
        FeatureFlagGate(
            feature = ApiFeatures.CustomisableBottomNavigation,
            disabled = {
                LegacyChatListEntry(key, navigationHandler::back)
            },
            enabled = {
                ChatListEntry(
                    navigationHandler = navigationHandler,
                    showMeetingTab = key.showMeetingTab,
                    createNewChat = key.createNewChat,
                )
            },
        )
    }
}

/**
 * Launches the legacy [ChatActivity] chat list and pops the entry.
 */
@Composable
private fun LegacyChatListEntry(
    key: ChatListNavKey,
    removeDestination: () -> Unit,
) {
    val context = LocalContext.current
    LaunchedEffect(Unit) {
        val intent = Intent(context, ChatActivity::class.java).apply {
            putExtra(ChatActivity.OPEN_CHAT_LIST, true)
            putExtra(ChatActivity.CREATE_NEW_CHAT, key.createNewChat)
            putExtra(ChatActivity.EXTRA_SHOW_MEETING_TAB, key.showMeetingTab)
        }
        context.startActivity(intent)
        removeDestination()
    }
}
