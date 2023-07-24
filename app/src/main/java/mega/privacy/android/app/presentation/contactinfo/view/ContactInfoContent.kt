package mega.privacy.android.app.presentation.contactinfo.view

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.heightIn
import androidx.compose.material.Divider
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import mega.privacy.android.app.R
import mega.privacy.android.app.presentation.contactinfo.model.ContactInfoState
import mega.privacy.android.core.ui.controls.controlssliders.MegaSwitch
import mega.privacy.android.core.ui.controls.lists.MenuActionListTile
import mega.privacy.android.core.ui.preview.CombinedThemePreviews
import mega.privacy.android.core.ui.theme.AndroidTheme
import mega.privacy.android.core.ui.theme.extensions.grey_alpha_012_white_alpha_012
import mega.privacy.android.domain.entity.ChatRoomPermission
import mega.privacy.android.domain.entity.chat.ChatRoom
import mega.privacy.android.domain.entity.contacts.ContactData
import mega.privacy.android.domain.entity.contacts.ContactItem
import mega.privacy.android.domain.entity.contacts.UserStatus
import mega.privacy.android.domain.entity.user.UserVisibility

@Composable
internal fun ContactInfoContent(
    uiState: ContactInfoState,
    modifier: Modifier = Modifier,
    contentHeight: Dp = 0.dp,
) = Column(modifier = modifier.heightIn(contentHeight)) {
    InfoOptionsView(
        primaryDisplayName = uiState.primaryDisplayName,
        secondaryDisplayName = uiState.secondaryDisplayName,
        modifyNickNameTextId = uiState.modifyNickNameTextId,
        email = uiState.email,
    )
    Divider(color = MaterialTheme.colors.grey_alpha_012_white_alpha_012)
    ChatOptions()
    Divider(color = MaterialTheme.colors.grey_alpha_012_white_alpha_012)
    MenuActionListTile(
        text = stringResource(id = R.string.title_incoming_shares_explorer),
        icon = R.drawable.ic_incoming_share,
    ) {
        Text(
            text = pluralStringResource(
                id = R.plurals.num_folders_with_parameter,
                count = uiState.inShares.size, uiState.inShares.size,
            ),
            style = MaterialTheme.typography.button,
            color = MaterialTheme.colors.secondary,
        )
    }
    MenuActionListTile(
        text = stringResource(id = R.string.title_properties_chat_notifications_contact),
        icon = R.drawable.ic_bell,
    ) {
        MegaSwitch(
            checked = true,
            onCheckedChange = {}
        )
    }
    MenuActionListTile(
        text = stringResource(id = R.string.title_properties_chat_share_contact),
        icon = R.drawable.ic_contact_share
    )
    VerifyCredentialsView(isVerified = uiState.areCredentialsVerified)
    if (uiState.chatRoom != null) {
        MenuActionListTile(
            text = stringResource(id = R.string.title_chat_shared_files_info),
            icon = R.drawable.ic_shared_files
        )
        MenuActionListTile(
            text = stringResource(id = R.string.title_properties_manage_chat),
            icon = R.drawable.ic_clear_chat_history,
        )
    }
    MenuActionListTile(
        text = stringResource(id = R.string.title_properties_remove_contact),
        icon = R.drawable.ic_remove_contact,
        isDestructive = true,
        addSeparator = false,
    )
}

@CombinedThemePreviews
@Composable
private fun PreviewContactInfoContent() {
    val contactData = ContactData(
        alias = "Iron Man",
        avatarUri = "https://avatar.uri.com",
        fullName = "Tony Stark",
    )
    AndroidTheme(isDark = isSystemInDarkTheme()) {
        ContactInfoContent(
            uiState = ContactInfoState(
                contactItem = ContactItem(
                    handle = 123456L,
                    email = "test@gmail.com",
                    contactData = contactData,
                    defaultAvatarColor = "red",
                    visibility = UserVisibility.Visible,
                    timestamp = 123456789,
                    areCredentialsVerified = true,
                    status = UserStatus.Online,
                    lastSeen = 0,
                )
            )
        )
    }
}

@CombinedThemePreviews
@Composable
private fun PreviewContactInfoContentWithChatRoom() {
    val contactData = ContactData(
        alias = "Iron Man",
        avatarUri = "https://avatar.uri.com",
        fullName = "Tony Stark",
    )
    val chatRoom = ChatRoom(
        chatId = 123456L,
        ownPrivilege = ChatRoomPermission.Moderator,
        numPreviewers = 12L,
        peerPrivilegesByHandles = emptyMap(),
        peerCount = 0L,
        peerHandlesList = emptyList(),
        isGroup = true,
        isPublic = true,
        isPreview = true,
        authorizationToken = null,
        title = "title",
        hasCustomTitle = true,
        unreadCount = 10,
        userTyping = 4L,
        userHandle = 123456789L,
        isActive = true,
        isArchived = true,
        retentionTime = 1234L,
        creationTime = 123465L,
        isMeeting = true,
        isWaitingRoom = true,
        isOpenInvite = true,
        isSpeakRequest = true,
        peerPrivilegesList = emptyList()
    )
    AndroidTheme(isDark = isSystemInDarkTheme()) {
        ContactInfoContent(
            uiState = ContactInfoState(
                contactItem = ContactItem(
                    handle = 123456L,
                    email = "test@gmail.com",
                    contactData = contactData,
                    defaultAvatarColor = "red",
                    visibility = UserVisibility.Visible,
                    timestamp = 123456789,
                    areCredentialsVerified = true,
                    status = UserStatus.Online,
                    lastSeen = 0,
                ),
                chatRoom = chatRoom,
            )
        )
    }
}
