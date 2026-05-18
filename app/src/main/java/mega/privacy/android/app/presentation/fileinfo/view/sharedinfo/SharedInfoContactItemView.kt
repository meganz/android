package mega.privacy.android.app.presentation.fileinfo.view.sharedinfo

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import mega.android.core.ui.components.contact.state.ContactItemStatus
import mega.android.core.ui.components.image.MegaIcon
import mega.android.core.ui.theme.values.IconColor
import mega.privacy.android.app.presentation.extensions.description
import mega.privacy.android.app.presentation.fileinfo.view.TEST_TAG_CONTACT_ITEM_SHARED
import mega.privacy.android.app.presentation.fileinfo.view.TEST_TAG_CONTACT_ITEM_SHARED_DOTS
import mega.privacy.android.domain.entity.shares.AccessPermission
import mega.privacy.android.icon.pack.IconPack
import mega.privacy.android.shared.contact.components.ContactItemView
import mega.privacy.android.shared.contact.model.AvatarData
import mega.privacy.android.shared.contact.model.ContactItemUiState
import mega.privacy.android.shared.contact.model.ContactPermissionUiState
import mega.privacy.android.shared.original.core.ui.preview.CombinedTextAndThemePreviews
import mega.privacy.android.shared.original.core.ui.theme.OriginalTheme

/**
 * Shared info contact item view
 *
 * @param contactItem
 * @param selected
 * @param onClick
 * @param onLongClick
 * @param onMoreOptionsClick
 * @param modifier
 */
@Composable
internal fun SharedInfoContactItemView(
    contactItem: ContactPermissionUiState,
    selected: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    onMoreOptionsClick: () -> Unit,
    modifier: Modifier = Modifier,
) = Row(
    verticalAlignment = Alignment.CenterVertically,
    modifier = modifier
        .fillMaxWidth()
        .testTag(TEST_TAG_CONTACT_ITEM_SHARED),
) {
    ContactItemView(
        displayName = contactItem.contactItemUiState.displayName,
        statusText = contactItem.permission.description()?.let {
            stringResource(id = it)
        } ?: "",
        onClick = onClick,
        onLongClick = onLongClick,
        modifier = Modifier.weight(1f),
        selected = selected,
        avatar = contactItem.contactItemUiState.avatar,
        status = contactItem.contactItemUiState.status,
        isVerified = contactItem.contactItemUiState.isVerified
    )
    IconButton(
        modifier = Modifier
            .testTag(TEST_TAG_CONTACT_ITEM_SHARED_DOTS),
        onClick = onMoreOptionsClick,
    ) {
        MegaIcon(
            painter = rememberVectorPainter(IconPack.Medium.Thin.Outline.MoreVertical),
            contentDescription = "More options",
            tint = IconColor.Secondary,
        )
    }
}

@CombinedTextAndThemePreviews
@Composable
private fun SharedInfoContactItemViewPreview() {
    val contactItem = ContactItemUiState(
        handle = 1L,
        displayName = "Alice Anderson",
        status = ContactItemStatus.Online,
        lastSeen = null,
        avatar = AvatarData.Initials(initials = "A", avatarColor = Color(0xFF2E7D32)),
        isVerified = true,
    )
    OriginalTheme(isDark = isSystemInDarkTheme()) {
        SharedInfoContactItemView(
            contactItem = ContactPermissionUiState(
                contactItemUiState = contactItem,
                email = "Alice@Anderson.com",
                permission = AccessPermission.READWRITE
            ),
            selected = false,
            onClick = {},
            onLongClick = {},
            onMoreOptionsClick = {},
        )
    }
}