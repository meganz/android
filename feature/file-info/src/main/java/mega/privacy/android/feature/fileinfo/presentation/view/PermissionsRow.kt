package mega.privacy.android.feature.fileinfo.presentation.view

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import mega.android.core.ui.components.MegaText
import mega.android.core.ui.components.image.MegaIcon
import mega.android.core.ui.preview.CombinedThemePreviews
import mega.android.core.ui.theme.AndroidThemeForPreviews
import mega.android.core.ui.theme.AppTheme
import mega.android.core.ui.theme.values.IconColor
import mega.android.core.ui.theme.values.TextColor
import mega.privacy.android.domain.entity.shares.AccessPermission
import mega.privacy.android.icon.pack.IconPack
import mega.privacy.android.shared.resources.R as sharedR

/**
 * The incoming-share access-level row: a "Permissions" label above the current permission (icon +
 * text). Read-only, so it is not clickable.
 *
 * @param accessPermission the current user's access level for the incoming share
 * @param modifier modifier for the row
 */
@Composable
internal fun PermissionsRow(
    accessPermission: AccessPermission,
    modifier: Modifier = Modifier,
) {
    // Only incoming-share access levels (read / read-write / full) have a display; anything else
    // (owner / unknown) has no permission to show, so the row is simply not emitted.
    accessPermission.permissionDisplay()?.let { permission ->
        Column(
            modifier = modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            MegaText(
                text = stringResource(sharedR.string.file_info_information_permissions_label),
                textColor = TextColor.Primary,
                style = AppTheme.typography.bodyLarge,
            )
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                MegaIcon(
                    modifier = Modifier.size(16.dp),
                    painter = rememberVectorPainter(permission.icon),
                    tint = IconColor.Secondary,
                    contentDescription = null,
                )
                MegaText(
                    text = permission.label,
                    textColor = TextColor.Secondary,
                    style = AppTheme.typography.bodyMedium,
                )
            }
        }
    }
}

private data class PermissionDisplay(val icon: ImageVector, val label: String)

@Composable
private fun AccessPermission.permissionDisplay(): PermissionDisplay? = when (this) {
    AccessPermission.READ -> PermissionDisplay(
        icon = IconPack.Medium.Thin.Outline.Eye,
        label = stringResource(sharedR.string.share_folder_dialog_read_only_radio_option),
    )

    AccessPermission.READWRITE -> PermissionDisplay(
        icon = IconPack.Medium.Thin.Outline.Edit,
        label = stringResource(sharedR.string.share_folder_dialog_read_write_radio_option),
    )

    AccessPermission.FULL -> PermissionDisplay(
        icon = IconPack.Medium.Thin.Outline.Star,
        label = stringResource(sharedR.string.share_folder_dialog_full_access_radio_option),
    )

    else -> null
}

@CombinedThemePreviews
@Composable
private fun PermissionsRowPreview() {
    AndroidThemeForPreviews {
        PermissionsRow(accessPermission = AccessPermission.FULL)
    }
}
