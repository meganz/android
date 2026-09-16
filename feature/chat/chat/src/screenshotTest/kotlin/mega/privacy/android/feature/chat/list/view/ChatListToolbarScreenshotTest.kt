package mega.privacy.android.feature.chat.list.view

import androidx.compose.runtime.Composable
import com.android.tools.screenshot.PreviewTest
import mega.android.core.ui.components.toolbar.AppBarNavigationType
import mega.android.core.ui.preview.CombinedThemePreviews
import mega.android.core.ui.theme.AndroidThemeForPreviews
import mega.privacy.android.domain.entity.chat.ChatPushNotificationMuteOption
import mega.privacy.android.domain.entity.chat.ChatStatus

/**
 * Screenshot tests for the chat list top app bar and the dialogs it opens: the presence subtitle
 * in each of its states, the presence picker, and the do-not-disturb mute period picker.
 */
class ChatListToolbarScreenshotTest {

    @PreviewTest
    @CombinedThemePreviews
    @Composable
    fun ChatListToolbarWithPresenceAndActions() {
        AndroidThemeForPreviews {
            ChatListTopAppBar(
                chatStatus = ChatStatus.Online,
                isSearchAvailable = true,
                hasArchivedChats = true,
                navigationType = AppBarNavigationType.None,
                onPresenceClick = {},
                onSearchClick = {},
                onArchivedClick = {},
                onOpenLinkClick = {},
                onDoNotDisturbClick = {},
            )
        }
    }

    @PreviewTest
    @CombinedThemePreviews
    @Composable
    fun ChatListToolbarWithoutSearchOrArchived() {
        AndroidThemeForPreviews {
            ChatListTopAppBar(
                chatStatus = ChatStatus.Away,
                isSearchAvailable = false,
                hasArchivedChats = false,
                navigationType = AppBarNavigationType.None,
                onPresenceClick = {},
                onSearchClick = {},
                onArchivedClick = {},
                onOpenLinkClick = {},
                onDoNotDisturbClick = {},
            )
        }
    }

    @PreviewTest
    @CombinedThemePreviews
    @Composable
    fun ChatListToolbarWithoutNetwork() {
        AndroidThemeForPreviews {
            ChatListToolbarWithPresence(ChatStatus.NoNetworkConnection)
        }
    }

    @PreviewTest
    @CombinedThemePreviews
    @Composable
    fun ChatListToolbarReconnecting() {
        AndroidThemeForPreviews {
            ChatListToolbarWithPresence(ChatStatus.Reconnecting)
        }
    }

    @PreviewTest
    @CombinedThemePreviews
    @Composable
    fun ChatListToolbarConnecting() {
        AndroidThemeForPreviews {
            ChatListToolbarWithPresence(ChatStatus.Connecting)
        }
    }

    @Composable
    private fun ChatListToolbarWithPresence(chatStatus: ChatStatus) {
        ChatListTopAppBar(
            chatStatus = chatStatus,
            isSearchAvailable = true,
            hasArchivedChats = true,
            navigationType = AppBarNavigationType.None,
            onPresenceClick = {},
            onSearchClick = {},
            onArchivedClick = {},
            onOpenLinkClick = {},
            onDoNotDisturbClick = {},
        )
    }

    @PreviewTest
    @CombinedThemePreviews
    @Composable
    fun ChatStatusPicker() {
        AndroidThemeForPreviews {
            ChatStatusDialog(
                chatStatus = ChatStatus.Online,
                onStatusSelected = {},
                onDismiss = {},
            )
        }
    }

    @PreviewTest
    @CombinedThemePreviews
    @Composable
    fun DoNotDisturbPicker() {
        AndroidThemeForPreviews {
            ChatDoNotDisturbDialog(
                options = listOf(
                    ChatPushNotificationMuteOption.Mute30Minutes,
                    ChatPushNotificationMuteOption.Mute1Hour,
                    ChatPushNotificationMuteOption.Mute6Hours,
                    ChatPushNotificationMuteOption.Mute24Hours,
                    ChatPushNotificationMuteOption.MuteUntilTomorrowMorning,
                ),
                onOptionSelected = {},
                onDismiss = {},
            )
        }
    }
}
