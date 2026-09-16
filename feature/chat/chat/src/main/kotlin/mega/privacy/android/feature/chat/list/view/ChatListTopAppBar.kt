package mega.privacy.android.feature.chat.list.view

import androidx.annotation.StringRes
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.size
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import mega.android.core.ui.components.image.MegaIcon
import mega.android.core.ui.components.toolbar.AppBarNavigationType
import mega.android.core.ui.components.toolbar.MegaTopAppBar
import mega.android.core.ui.model.menu.MenuAction
import mega.android.core.ui.model.menu.MenuActionWithoutIcon
import mega.android.core.ui.preview.CombinedThemePreviews
import mega.android.core.ui.theme.AndroidThemeForPreviews
import mega.android.core.ui.theme.values.IconColor
import mega.privacy.android.domain.entity.chat.ChatStatus
import mega.privacy.android.icon.pack.IconPack
import mega.privacy.android.shared.resources.R as sharedR

/**
 * Chat list top app bar shown while neither search nor multi-select is active.
 *
 * It carries the user's presence as its subtitle and opens the presence picker when tapped, offers
 * search and the archived chats list only while either has anything to show, and keeps the open-link
 * and do-not-disturb shortcuts in its overflow.
 *
 * @param chatStatus Presence the user is currently publishing, or null while it is still being
 * resolved; the subtitle is then omitted.
 * @param isSearchAvailable Whether there is anything worth searching.
 * @param hasArchivedChats Whether the user has any archived chat.
 * @param navigationType Navigation affordance of the app bar.
 * @param onPresenceClick Called when the user asks to change their presence.
 * @param onSearchClick Called when the search affordance is pressed.
 * @param onArchivedClick Called when the archived chats affordance is pressed.
 * @param onOpenLinkClick Called when the open-link action is picked.
 * @param onDoNotDisturbClick Called when the do-not-disturb action is picked.
 * @param modifier [Modifier]
 */
@Composable
internal fun ChatListTopAppBar(
    chatStatus: ChatStatus?,
    isSearchAvailable: Boolean,
    hasArchivedChats: Boolean,
    navigationType: AppBarNavigationType,
    onPresenceClick: () -> Unit,
    onSearchClick: () -> Unit,
    onArchivedClick: () -> Unit,
    onOpenLinkClick: () -> Unit,
    onDoNotDisturbClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    MegaTopAppBar(
        modifier = modifier
            .clickable(
                onClickLabel = stringResource(sharedR.string.status_label),
                onClick = onPresenceClick,
            )
            .testTag(CHAT_LIST_TOP_BAR_TAG),
        title = stringResource(sharedR.string.general_chats_label),
        subtitle = chatStatus?.labelId()?.let { stringResource(it) },
        navigationType = navigationType,
        trailingIcons = {
            if (isSearchAvailable) {
                ChatListTopAppBarIcon(
                    testTag = CHAT_LIST_SEARCH_ACTION_TAG,
                    icon = IconPack.Medium.Thin.Outline.SearchLarge,
                    contentDescription = stringResource(sharedR.string.search_placeholder_chat),
                    onClick = onSearchClick,
                )
            }
            if (hasArchivedChats) {
                ChatListTopAppBarIcon(
                    testTag = CHAT_LIST_ARCHIVED_TAG,
                    icon = IconPack.Medium.Thin.Outline.Archive,
                    contentDescription = stringResource(sharedR.string.chat_archived_chats_title),
                    onClick = onArchivedClick,
                )
            }
        },
        actions = listOf(ChatListMenuAction.OpenLink, ChatListMenuAction.DoNotDisturb),
        onActionPressed = { action ->
            when (action) {
                ChatListMenuAction.OpenLink -> onOpenLinkClick()
                ChatListMenuAction.DoNotDisturb -> onDoNotDisturbClick()
                else -> Unit
            }
        },
    )
}

@Composable
private fun RowScope.ChatListTopAppBarIcon(
    testTag: String,
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
) {
    IconButton(
        modifier = Modifier.testTag(testTag),
        onClick = onClick,
    ) {
        MegaIcon(
            modifier = Modifier.size(24.dp),
            painter = rememberVectorPainter(icon),
            contentDescription = contentDescription,
            tint = IconColor.Primary,
        )
    }
}

/**
 * Overflow actions of the chat list top app bar.
 */
internal sealed interface ChatListMenuAction : MenuAction {

    /**
     * Opens any MEGA link pasted by the user.
     */
    data object OpenLink : MenuActionWithoutIcon(
        descriptionRes = sharedR.string.open_link_dialog_title,
        testTag = CHAT_LIST_OPEN_LINK_TAG,
    ), ChatListMenuAction {
        override val orderInCategory = 110
    }

    /**
     * Silences notifications for every chat at once.
     */
    data object DoNotDisturb : MenuActionWithoutIcon(
        descriptionRes = sharedR.string.title_dialog_mute_chat_notifications,
        testTag = CHAT_LIST_DO_NOT_DISTURB_TAG,
    ), ChatListMenuAction {
        override val orderInCategory = 140
    }
}

@StringRes
private fun ChatStatus.labelId(): Int = when (this) {
    ChatStatus.Online -> sharedR.string.online_status
    ChatStatus.Away -> sharedR.string.away_status
    ChatStatus.Busy -> sharedR.string.busy_status
    ChatStatus.Offline -> sharedR.string.offline_status
    ChatStatus.NoNetworkConnection -> sharedR.string.error_server_connection_problem
    ChatStatus.Reconnecting -> sharedR.string.invalid_connection_state
    ChatStatus.Connecting -> sharedR.string.chat_connecting
}

internal const val CHAT_LIST_TOP_BAR_TAG = "chat_list_top_app_bar:bar"
internal const val CHAT_LIST_SEARCH_ACTION_TAG = "chat_list_top_app_bar:search_action"
internal const val CHAT_LIST_ARCHIVED_TAG = "chat_list_top_app_bar:archived_action"
internal const val CHAT_LIST_OPEN_LINK_TAG = "chat_list_top_app_bar:open_link_action"
internal const val CHAT_LIST_DO_NOT_DISTURB_TAG = "chat_list_top_app_bar:do_not_disturb_action"

@CombinedThemePreviews
@Composable
private fun ChatListTopAppBarPreview() {
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

@CombinedThemePreviews
@Composable
private fun ChatListTopAppBarWithoutActionsPreview() {
    AndroidThemeForPreviews {
        ChatListTopAppBar(
            chatStatus = null,
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
