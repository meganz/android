package mega.privacy.android.app.presentation.fileinfo.view

import mega.privacy.android.icon.pack.R as iconPackR
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.dp
import mega.privacy.android.app.R
import mega.privacy.android.app.presentation.contact.view.ContactItemView
import mega.privacy.android.app.presentation.extensions.description
import mega.privacy.android.domain.entity.contacts.ContactData
import mega.privacy.android.domain.entity.contacts.ContactItem
import mega.privacy.android.domain.entity.contacts.ContactPermission
import mega.privacy.android.domain.entity.contacts.UserChatStatus
import mega.privacy.android.domain.entity.shares.AccessPermission
import mega.privacy.android.domain.entity.user.UserVisibility
import mega.privacy.android.shared.original.core.ui.controls.dividers.DividerType
import mega.privacy.android.shared.original.core.ui.controls.lists.MenuActionListTile
import mega.privacy.android.shared.original.core.ui.preview.BooleanProvider
import mega.privacy.android.shared.original.core.ui.preview.CombinedThemePreviews
import mega.privacy.android.shared.original.core.ui.theme.OriginalTempTheme

/**
 * Content for file contacts list bottom sheet
 */
@Composable
fun ColumnScope.ShareContactOptionsContent(
    contactPermission: ContactPermission,
    allowChangePermission: Boolean,
    onInfoClicked: () -> Unit,
    onChangePermissionClicked: () -> Unit,
    onRemoveClicked: () -> Unit,
) {
    ContactItemView(
        contactItem = contactPermission.contactItem,
        onClick = null,
        statusOverride = contactPermission.accessPermission.description()?.let {
            stringResource(id = it)
        } ?: "",
        dividerType = DividerType.SmallStartPadding,
        modifier = Modifier.padding(vertical = 8.dp)
    )
    MenuActionListTile(
        text = stringResource(id = R.string.general_info),
        icon = painterResource(id = mega.privacy.android.icon.pack.R.drawable.ic_info_medium_regular_outline),
        onActionClicked = onInfoClicked,
    )
    if (allowChangePermission) {
        MenuActionListTile(
            text = stringResource(id = R.string.file_properties_shared_folder_change_permissions),
            icon = painterResource(id = iconPackR.drawable.ic_key_02_medium_regular_outline),
            onActionClicked = onChangePermissionClicked,
        )
    }
    MenuActionListTile(
        text = stringResource(id = R.string.context_remove),
        icon = painterResource(id = iconPackR.drawable.ic_x_medium_regular_outline),
        isDestructive = true,
        onActionClicked = onRemoveClicked,
    )
}

@CombinedThemePreviews
@Composable
private fun FileContactsListBottomSheetContentPreview(
    @PreviewParameter(BooleanProvider::class) allowChangePermission: Boolean,
) = Column(modifier = Modifier.sizeIn(minHeight = 200.dp)) {
    OriginalTempTheme(isDark = isSystemInDarkTheme()) {
        val contactItem = ContactItem(
            1L,
            "email@example.com",
            ContactData(null, null, null),
            "red",
            UserVisibility.Visible,
            0L,
            true,
            UserChatStatus.Online,
            null,
            null
        )
        val contactPermission = ContactPermission(contactItem, AccessPermission.READWRITE)
        ShareContactOptionsContent(contactPermission = contactPermission, allowChangePermission, {}, {}, {})
    }
}