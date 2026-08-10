package mega.privacy.android.app.presentation.meeting.chat.navigation

import androidx.activity.compose.LocalActivity
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import mega.privacy.android.app.presentation.chat.list.ChatTabsScreen
import mega.privacy.android.app.utils.Constants
import mega.privacy.android.navigation.contract.NavigationHandler

/**
 * Registers the chat tabs (Chats / Meetings) entry inside the
 * [mega.privacy.android.app.appstate.content.navigation.LegacyActivityScaffold] hosted by
 * [mega.privacy.android.app.presentation.meeting.chat.ChatActivity].
 *
 * Replaces the former `ChatTabsFragment`.
 *
 * @param navigationHandler The scaffold navigation handler. Used to open a chat conversation on top
 * of the tabs without starting a new Activity.
 */
internal fun EntryProviderScope<NavKey>.chatTabsDestination(
    navigationHandler: NavigationHandler,
) {
    entry<ChatTabsContainerNavKey> { key ->
        val activity = LocalActivity.current
        ChatTabsScreen(
            showMeetingTab = key.showMeetingTab,
            createNewChat = key.createNewChat,
            onNavigateToChat = { chatId ->
                navigationHandler.openChatConversation(
                    ChatLegacyContainerNavKey(
                        chatId = chatId,
                        action = Constants.ACTION_CHAT_SHOW_MESSAGES,
                    )
                )
            },
            onFinish = { activity?.finish() },
        )
    }
}
