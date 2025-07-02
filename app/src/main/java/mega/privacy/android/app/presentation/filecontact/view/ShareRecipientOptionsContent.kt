package mega.privacy.android.app.presentation.filecontact.view

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTagsAsResourceId
import androidx.compose.ui.unit.dp
import mega.android.core.ui.components.MegaText
import mega.android.core.ui.components.image.MegaIcon
import mega.android.core.ui.preview.CombinedThemePreviews
import mega.android.core.ui.theme.AndroidThemeForPreviews
import mega.android.core.ui.theme.values.TextColor
import mega.privacy.android.app.R
import mega.privacy.android.domain.entity.contacts.ContactData
import mega.privacy.android.domain.entity.contacts.UserChatStatus
import mega.privacy.android.domain.entity.shares.AccessPermission
import mega.privacy.android.domain.entity.shares.ShareRecipient
import mega.privacy.android.domain.entity.user.UserVisibility
import mega.privacy.android.icon.pack.IconPack


/**
 * Content for file contacts list bottom sheet
 */
@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun ColumnScope.ShareRecipientsOptionsContent(
    recipient: ShareRecipient,
    allowChangePermission: Boolean,
    onInfoClicked: (() -> Unit)?,
    onChangePermissionClicked: () -> Unit,
    onRemoveClicked: () -> Unit,
) {
    ShareRecipientView(
        shareRecipient = recipient,
        modifier = Modifier
            .semantics { testTagsAsResourceId = true }
            .testTag(SHARE_CONTACT_OPTIONS_TITLE)
            .padding(vertical = 8.dp)
    )
    Actions(
        onInfoClicked = onInfoClicked,
        allowChangePermission = allowChangePermission,
        onChangePermissionClicked = onChangePermissionClicked,
        onRemoveClicked = onRemoveClicked,
        modifier = Modifier
            .height(56.dp)
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
    )
}

@Composable
private fun Actions(
    onInfoClicked: (() -> Unit)?,
    allowChangePermission: Boolean,
    onChangePermissionClicked: () -> Unit,
    onRemoveClicked: () -> Unit,
    modifier: Modifier = Modifier,
) {
    onInfoClicked?.let {
        ActionItem(
            icon = IconPack.Medium.Regular.Outline.Info,
            text = stringResource(id = R.string.general_info),
            onClickListener = it,
            modifier = modifier.testTag(SHARE_CONTACT_OPTIONS_INFO),
        )
    }
    if (allowChangePermission) {
        ActionItem(
            text = stringResource(id = R.string.file_properties_shared_folder_change_permissions),
            icon = IconPack.Medium.Regular.Outline.Key02,
            onClickListener = onChangePermissionClicked,
            modifier = modifier.testTag(SHARE_CONTACT_OPTIONS_CHANGE_PERMISSION),
        )
    }
    ActionItem(
        text = stringResource(id = R.string.context_remove),
        icon = IconPack.Medium.Regular.Outline.X,
        iconTint = TextColor.Warning,
        textColor = TextColor.Warning,
        onClickListener = onRemoveClicked,
        modifier = modifier.testTag(SHARE_CONTACT_OPTIONS_REMOVE),
    )
}

@Composable
private fun ActionItem(
    onClickListener: () -> Unit,
    icon: ImageVector,
    text: String,
    modifier: Modifier = Modifier,
    iconTint: TextColor = TextColor.Primary,
    textColor: TextColor = TextColor.Primary,
) {
    Row(
        horizontalArrangement = Arrangement.Start,
        modifier = modifier
            .clickable(onClick = onClickListener)
            .height(56.dp)
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        MegaIcon(
            modifier = Modifier
                .size(24.dp)
                .align(Alignment.CenterVertically),
            painter = rememberVectorPainter(icon),
            textColorTint = iconTint
        )
        MegaText(
            text = text,
            textColor = textColor,
            modifier = Modifier
                .padding(start = 16.dp)
                .align(Alignment.CenterVertically)
        )
    }
}

internal const val SHARE_CONTACT_OPTIONS_TITLE = "share_contact_options:title_contact_item"
internal const val SHARE_CONTACT_OPTIONS_INFO = "share_contact_options:info_list_item"
internal const val SHARE_CONTACT_OPTIONS_CHANGE_PERMISSION =
    "share_contact_options:change_permission_list_item"
internal const val SHARE_CONTACT_OPTIONS_REMOVE = "share_contact_options:remove_list_item"

@CombinedThemePreviews
@Composable
private fun FileContactListOptionsBottomSheetPreview() {
    AndroidThemeForPreviews {

        val contact = ShareRecipient.Contact(
            handle = 1L,
            email = "contact@email.com",
            contactData = ContactData(
                fullName = "Contact Name",
                alias = "Contact Alias",
                avatarUri = null,
                userVisibility = UserVisibility.Visible,
            ),
            isVerified = true,
            permission = AccessPermission.READ,
            isPending = false,
            status = UserChatStatus.Online,
            defaultAvatarColor = 0,
        )

        Column {
            ShareRecipientsOptionsContent(
                recipient = contact,
                allowChangePermission = true,
                onInfoClicked = { },
                onChangePermissionClicked = {},
                onRemoveClicked = {},
            )
        }
    }
}
