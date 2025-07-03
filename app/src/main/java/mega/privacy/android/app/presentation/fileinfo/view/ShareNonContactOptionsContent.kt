package mega.privacy.android.app.presentation.fileinfo.view

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.runtime.Composable
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTagsAsResourceId
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.dp
import mega.privacy.android.app.R
import mega.privacy.android.app.presentation.contact.model.ContactAvatar
import mega.privacy.android.app.presentation.contact.model.ContactStatus
import mega.privacy.android.app.presentation.contact.model.ContactUiItem
import mega.privacy.android.app.presentation.contact.view.ContactItemView
import mega.privacy.android.app.presentation.extensions.description
import mega.privacy.android.app.presentation.extensions.getAvatarFirstLetter
import mega.privacy.android.domain.entity.shares.AccessPermission
import mega.privacy.android.icon.pack.IconPack
import mega.privacy.android.shared.original.core.ui.controls.dividers.DividerType
import mega.privacy.android.shared.original.core.ui.controls.lists.MenuActionListTile
import mega.privacy.android.shared.original.core.ui.preview.BooleanProvider
import mega.privacy.android.shared.original.core.ui.preview.CombinedThemePreviews
import mega.privacy.android.shared.original.core.ui.theme.OriginalTheme

/**
 * Content for file contacts list bottom sheet for a Pending contact
 */
@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun ColumnScope.ShareNonContactOptionsContent(
    nonContactEmail: String,
    accessPermission: AccessPermission,
    avatarColor: Int,
    allowChangePermission: Boolean,
    onChangePermissionClicked: () -> Unit,
    onRemoveClicked: () -> Unit,
) {
    val uiItem = ContactUiItem(
        nameOrEmail = nonContactEmail,
        contactStatus = ContactStatus(null, null),
        avatar = ContactAvatar.InitialsAvatar(
            firstLetter = getAvatarFirstLetter(nonContactEmail),
            defaultAvatarColor = Color(avatarColor),
            areCredentialsVerified = false,
        )
    )
    ContactItemView(
        contactUiItem = uiItem,
        onClick = null,
        modifier = Modifier
            .semantics { testTagsAsResourceId = true }
            .testTag(SHARE_NON_CONTACT_OPTIONS_TITLE)
            .padding(vertical = 8.dp),
        statusOverride = accessPermission.description()?.let {
            stringResource(id = it)
        } ?: "",
        dividerType = DividerType.SmallStartPadding,
    )
    if (allowChangePermission) {
        MenuActionListTile(
            text = stringResource(id = R.string.file_properties_shared_folder_change_permissions),
            icon = rememberVectorPainter(IconPack.Medium.Regular.Outline.Key02),
            onActionClicked = onChangePermissionClicked,
            modifier = Modifier.testTag(SHARE_NON_CONTACT_OPTIONS_CHANGE_PERMISSION),
        )
    }
    MenuActionListTile(
        text = stringResource(id = R.string.context_remove),
        icon = rememberVectorPainter(IconPack.Medium.Regular.Outline.X),
        isDestructive = true,
        onActionClicked = onRemoveClicked,
        modifier = Modifier.testTag(SHARE_NON_CONTACT_OPTIONS_REMOVE),
    )
}

@CombinedThemePreviews
@Composable
private fun ShareNonContactOptionsContentPreview(
    @PreviewParameter(BooleanProvider::class) allowChangePermission: Boolean,
) = OriginalTheme(isDark = isSystemInDarkTheme()) {
    Column(modifier = Modifier.sizeIn(minHeight = 200.dp)) {
        ShareNonContactOptionsContent(
            nonContactEmail = "xyz@mega.co.nz",
            accessPermission = AccessPermission.READ,
            avatarColor = 2,
            allowChangePermission = allowChangePermission,
            onChangePermissionClicked = {},
            onRemoveClicked = {},
        )
    }
}

internal const val SHARE_NON_CONTACT_OPTIONS_TITLE = "share_non_contact_options:title"
internal const val SHARE_NON_CONTACT_OPTIONS_CHANGE_PERMISSION =
    "share_non_contact_options:change_permission"
internal const val SHARE_NON_CONTACT_OPTIONS_REMOVE = "share_non_contact_options:remove"
