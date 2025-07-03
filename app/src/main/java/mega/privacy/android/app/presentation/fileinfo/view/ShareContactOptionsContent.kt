package mega.privacy.android.app.presentation.fileinfo.view

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.runtime.Composable
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTagsAsResourceId
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.dp
import mega.privacy.android.app.R
import mega.privacy.android.app.presentation.contact.view.ContactItemView
import mega.privacy.android.app.presentation.extensions.description
import mega.privacy.android.app.presentation.preview.contactItemForPreviews
import mega.privacy.android.domain.entity.contacts.ContactPermission
import mega.privacy.android.domain.entity.shares.AccessPermission
import mega.privacy.android.icon.pack.IconPack
import mega.privacy.android.shared.original.core.ui.controls.dividers.DividerType
import mega.privacy.android.shared.original.core.ui.controls.lists.MenuActionListTile
import mega.privacy.android.shared.original.core.ui.preview.BooleanProvider
import mega.privacy.android.shared.original.core.ui.preview.CombinedThemePreviews
import mega.privacy.android.shared.original.core.ui.theme.OriginalTheme

/**
 * Content for file contacts list bottom sheet
 */
@OptIn(ExperimentalComposeUiApi::class)
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
        modifier = Modifier
            .semantics { testTagsAsResourceId = true }
            .testTag(SHARE_CONTACT_OPTIONS_TITLE)
            .padding(vertical = 8.dp)
    )
    MenuActionListTile(
        text = stringResource(id = R.string.general_info),
        icon = rememberVectorPainter(IconPack.Medium.Regular.Outline.Info),
        onActionClicked = onInfoClicked,
        modifier = Modifier.testTag(SHARE_CONTACT_OPTIONS_INFO),
    )
    if (allowChangePermission) {
        MenuActionListTile(
            text = stringResource(id = R.string.file_properties_shared_folder_change_permissions),
            icon = rememberVectorPainter(IconPack.Medium.Regular.Outline.Key02),
            onActionClicked = onChangePermissionClicked,
            modifier = Modifier.testTag(SHARE_CONTACT_OPTIONS_CHANGE_PERMISSION),
        )
    }
    MenuActionListTile(
        text = stringResource(id = R.string.context_remove),
        icon = rememberVectorPainter(IconPack.Medium.Regular.Outline.X),
        isDestructive = true,
        onActionClicked = onRemoveClicked,
        modifier = Modifier.testTag(SHARE_CONTACT_OPTIONS_REMOVE),
    )
}

internal const val SHARE_CONTACT_OPTIONS_TITLE = "share_contact_options:title_contact_item"
internal const val SHARE_CONTACT_OPTIONS_INFO = "share_contact_options:info_list_item"
internal const val SHARE_CONTACT_OPTIONS_CHANGE_PERMISSION =
    "share_contact_options:change_permission_list_item"
internal const val SHARE_CONTACT_OPTIONS_REMOVE = "share_contact_options:remove_list_item"

@CombinedThemePreviews
@Composable
private fun FileContactsListBottomSheetContentPreview(
    @PreviewParameter(BooleanProvider::class) allowChangePermission: Boolean,
) = Column(modifier = Modifier.sizeIn(minHeight = 200.dp)) {
    OriginalTheme(isDark = isSystemInDarkTheme()) {
        val contactPermission = contactPermissionForPreview
        ShareContactOptionsContent(
            contactPermission = contactPermission,
            allowChangePermission,
            {},
            {},
            {})
    }
}

internal val contactPermissionForPreview by lazy {
    ContactPermission(contactItemForPreviews, AccessPermission.READWRITE)
}