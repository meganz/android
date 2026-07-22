package mega.privacy.android.feature.contact.info.view

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import de.palm.composestateevents.consumed
import mega.android.core.ui.components.divider.SubtleDivider
import mega.android.core.ui.preview.CombinedThemePreviews
import mega.android.core.ui.theme.AndroidThemeForPreviews
import mega.privacy.android.domain.entity.contacts.UserChatStatus
import mega.privacy.android.feature.contact.info.model.ContactInfoUiState
import mega.privacy.android.shared.contact.model.AvatarData

/**
 * Content of the contact info screen for a resolved contact: header, chat action bar and the
 * option/chat rows, in a scrollable column.
 *
 * @param state
 * @param onSendMessageClick
 * @param onStartAudioCallClick
 * @param onStartVideoCallClick
 * @param onNicknameClick
 * @param onVerifyCredentialsClick
 * @param onShareContactClick
 * @param onSharedFoldersClick
 * @param onNotificationToggled invoked with the new checked value when the notifications toggle
 * is switched.
 * @param onSharedFilesClick
 * @param onManageChatHistoryClick
 * @param onRemoveContactClick
 * @param modifier
 */
@Composable
internal fun ContactInfoContent(
    state: ContactInfoUiState.Data,
    onSendMessageClick: () -> Unit,
    onStartAudioCallClick: () -> Unit,
    onStartVideoCallClick: () -> Unit,
    onNicknameClick: () -> Unit,
    onVerifyCredentialsClick: () -> Unit,
    onShareContactClick: () -> Unit,
    onSharedFoldersClick: () -> Unit,
    onNotificationToggled: (Boolean) -> Unit,
    onSharedFilesClick: () -> Unit,
    onManageChatHistoryClick: () -> Unit,
    onRemoveContactClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .testTag(CONTACT_INFO_CONTENT_TAG),
    ) {
        ContactInfoHeader(
            avatar = state.avatar,
            displayName = state.displayName,
            userChatStatus = state.userChatStatus,
            lastSeenMinutes = state.lastSeenMinutes,
            nickname = state.nickname,
            email = state.email,
        )
        if (state.showChatOptions) {
            SubtleDivider()
            ContactInfoActionBar(
                callButtonsEnabled = state.enableCallButtons,
                onSendMessageClick = onSendMessageClick,
                onStartAudioCallClick = onStartAudioCallClick,
                onStartVideoCallClick = onStartVideoCallClick,
            )
        }
        SubtleDivider()
        ContactInfoOptionsSection(
            nickname = state.nickname,
            areCredentialsVerified = state.areCredentialsVerified,
            showVerifyCredentials = state.showVerifyCredentials,
            showShareContact = state.showShareContact,
            showSharedFolders = state.showSharedFolders,
            sharedFoldersCount = state.inSharesCount,
            onNicknameClick = onNicknameClick,
            onVerifyCredentialsClick = onVerifyCredentialsClick,
            onShareContactClick = onShareContactClick,
            onSharedFoldersClick = onSharedFoldersClick,
        )
        SubtleDivider()
        ContactInfoChatSection(
            isNotificationEnabled = state.isNotificationEnabled,
            retentionTimeSeconds = state.retentionTimeSeconds,
            showSharedFiles = state.showSharedFiles,
            showManageChatHistory = state.showManageChatHistory,
            onNotificationToggled = onNotificationToggled,
            onSharedFilesClick = onSharedFilesClick,
            onManageChatHistoryClick = onManageChatHistoryClick,
            onRemoveContactClick = onRemoveContactClick,
        )
    }
}

internal const val CONTACT_INFO_CONTENT_TAG = "contact_info_content"

@CombinedThemePreviews
@Composable
private fun ContactInfoContentPreview() {
    AndroidThemeForPreviews {
        ContactInfoContent(
            state = ContactInfoUiState.Data(
                displayName = "Alice Anderson",
                nickname = "Ally",
                email = "alice@example.com",
                userHandle = 1L,
                chatRoomId = 123L,
                isFromContacts = true,
                avatar = AvatarData.Initials(initials = "A", avatarColor = Color(0xFF2E7D32)),
                userChatStatus = UserChatStatus.Online,
                lastSeenMinutes = null,
                areCredentialsVerified = true,
                isNotificationEnabled = true,
                retentionTimeSeconds = SECONDS_IN_DAY,
                inSharesCount = 3,
                enableCallButtons = true,
                isOnline = true,
                closeEvent = consumed,
            ),
            onSendMessageClick = {},
            onStartAudioCallClick = {},
            onStartVideoCallClick = {},
            onNicknameClick = {},
            onVerifyCredentialsClick = {},
            onShareContactClick = {},
            onSharedFoldersClick = {},
            onNotificationToggled = {},
            onSharedFilesClick = {},
            onManageChatHistoryClick = {},
            onRemoveContactClick = {},
        )
    }
}
