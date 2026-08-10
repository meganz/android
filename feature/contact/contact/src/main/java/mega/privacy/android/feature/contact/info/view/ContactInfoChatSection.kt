package mega.privacy.android.feature.contact.info.view

import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import mega.android.core.ui.components.divider.SubtleDivider
import mega.android.core.ui.components.list.FlexibleLineListItem
import mega.android.core.ui.components.list.OneLineListItem
import mega.android.core.ui.components.toggle.Toggle
import mega.android.core.ui.preview.CombinedThemePreviews
import mega.android.core.ui.theme.AndroidThemeForPreviews
import mega.android.core.ui.theme.values.TextColor
import mega.privacy.android.shared.resources.R as sharedR

/**
 * Chat-related rows of the contact info screen: chat notifications, shared files, manage chat
 * history and remove contact.
 *
 * @param isNotificationEnabled true when chat notifications are enabled, false when muted, null
 * when unknown (rendered as enabled).
 * @param retentionTimeSeconds chat history retention time in seconds; the manage chat history row
 * shows it as a subtitle when set.
 * @param showSharedFiles whether the chat shared files row is visible.
 * @param showManageChatHistory whether the manage chat history row is visible.
 * @param onNotificationToggled invoked with the new checked value when the toggle is switched.
 * @param onSharedFilesClick
 * @param onManageChatHistoryClick
 * @param onRemoveContactClick
 * @param modifier
 */
@Composable
internal fun ContactInfoChatSection(
    isNotificationEnabled: Boolean?,
    retentionTimeSeconds: Long?,
    showSharedFiles: Boolean,
    showManageChatHistory: Boolean,
    onNotificationToggled: (Boolean) -> Unit,
    onSharedFilesClick: () -> Unit,
    onManageChatHistoryClick: () -> Unit,
    onRemoveContactClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.testTag(CONTACT_INFO_CHAT_SECTION_TAG)) {
        FlexibleLineListItem(
            modifier = Modifier.testTag(CONTACT_INFO_NOTIFICATIONS_ROW_TAG),
            title = stringResource(sharedR.string.contact_info_chat_notifications),
            subtitle = if (isNotificationEnabled == false) {
                stringResource(sharedR.string.contact_info_notifications_muted)
            } else {
                null
            },
            enableClick = false,
            trailingElement = {
                Toggle(
                    modifier = Modifier.testTag(CONTACT_INFO_NOTIFICATIONS_TOGGLE_TAG),
                    isChecked = isNotificationEnabled != false,
                    onCheckedChange = onNotificationToggled,
                )
            },
        )
        if (showSharedFiles) {
            SubtleDivider()
            OneLineListItem(
                modifier = Modifier.testTag(CONTACT_INFO_SHARED_FILES_ROW_TAG),
                text = stringResource(sharedR.string.contact_info_shared_files),
                onClickListener = onSharedFilesClick,
            )
        }
        if (showManageChatHistory) {
            SubtleDivider()
            FlexibleLineListItem(
                modifier = Modifier.testTag(CONTACT_INFO_MANAGE_CHAT_HISTORY_ROW_TAG),
                title = stringResource(sharedR.string.contact_info_manage_chat_history),
                subtitle = retentionTimeSeconds?.let { getRetentionTimeString(it) }
                    ?.let { retentionTime ->
                        "${stringResource(sharedR.string.contact_info_manage_chat_history_subtitle)} $retentionTime"
                    },
                onClickListener = onManageChatHistoryClick,
            )
        }
        SubtleDivider()
        FlexibleLineListItem(
            modifier = Modifier.testTag(CONTACT_INFO_REMOVE_CONTACT_ROW_TAG),
            title = stringResource(sharedR.string.contacts_action_remove_contact),
            titleTextColor = TextColor.Error,
            onClickListener = onRemoveContactClick,
        )
    }
}

internal const val CONTACT_INFO_CHAT_SECTION_TAG = "contact_info_chat_section"
internal const val CONTACT_INFO_NOTIFICATIONS_ROW_TAG =
    "contact_info_chat_section:row_notifications"
internal const val CONTACT_INFO_NOTIFICATIONS_TOGGLE_TAG =
    "contact_info_chat_section:toggle_notifications"
internal const val CONTACT_INFO_SHARED_FILES_ROW_TAG =
    "contact_info_chat_section:row_shared_files"
internal const val CONTACT_INFO_MANAGE_CHAT_HISTORY_ROW_TAG =
    "contact_info_chat_section:row_manage_chat_history"
internal const val CONTACT_INFO_REMOVE_CONTACT_ROW_TAG =
    "contact_info_chat_section:row_remove_contact"

@CombinedThemePreviews
@Composable
private fun ContactInfoChatSectionPreview() {
    AndroidThemeForPreviews {
        ContactInfoChatSection(
            isNotificationEnabled = true,
            retentionTimeSeconds = SECONDS_IN_DAY,
            showSharedFiles = true,
            showManageChatHistory = true,
            onNotificationToggled = {},
            onSharedFilesClick = {},
            onManageChatHistoryClick = {},
            onRemoveContactClick = {},
        )
    }
}

@CombinedThemePreviews
@Composable
private fun ContactInfoChatSectionMutedNoChatPreview() {
    AndroidThemeForPreviews {
        ContactInfoChatSection(
            isNotificationEnabled = false,
            retentionTimeSeconds = null,
            showSharedFiles = false,
            showManageChatHistory = false,
            onNotificationToggled = {},
            onSharedFilesClick = {},
            onManageChatHistoryClick = {},
            onRemoveContactClick = {},
        )
    }
}
