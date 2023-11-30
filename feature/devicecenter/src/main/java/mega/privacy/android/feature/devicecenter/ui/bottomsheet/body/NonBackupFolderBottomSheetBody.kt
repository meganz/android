package mega.privacy.android.feature.devicecenter.ui.bottomsheet.body

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import mega.privacy.android.shared.theme.MegaAppTheme
import mega.privacy.android.core.ui.preview.CombinedThemePreviews
import mega.privacy.android.feature.devicecenter.ui.bottomsheet.tiles.InfoBottomSheetTile
import mega.privacy.android.feature.devicecenter.ui.bottomsheet.tiles.ShowInCloudDriveBottomSheetTile

/**
 * Test Tag for the Non Backup Folder Bottom Sheet Body
 */
internal const val BOTTOM_SHEET_BODY_NON_BACKUP_FOLDER =
    "non_backup_folder_bottom_sheet_body:column_options_list"

/**
 * A [Composable] Bottom Sheet Body that displays options specific to a Device Folder
 *
 * @param onShowInCloudDriveClicked Lambda that is executed when the "Show in Cloud Drive" Tile is
 * selected
 * @param onInfoClicked Lambda that is executed when the "Info" Tile is selected
 */
@Composable
internal fun NonBackupFolderBottomSheetBody(
    onShowInCloudDriveClicked: () -> Unit,
    onInfoClicked: () -> Unit,
) {
    Column(modifier = Modifier.testTag(BOTTOM_SHEET_BODY_NON_BACKUP_FOLDER)) {
        InfoBottomSheetTile(onActionClicked = onInfoClicked)
        ShowInCloudDriveBottomSheetTile(onActionClicked = onShowInCloudDriveClicked)
    }
}

/**
 * A Preview Composable that displays the Bottom Sheet Bottom and its Options for a Non Backup Folder
 */
@CombinedThemePreviews
@Composable
private fun PreviewNonBackupFolderBottomSheet() {
    MegaAppTheme(isDark = isSystemInDarkTheme()) {
        NonBackupFolderBottomSheetBody(
            onShowInCloudDriveClicked = {},
            onInfoClicked = {},
        )
    }
}