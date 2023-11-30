package mega.privacy.android.feature.devicecenter.ui.bottomsheet.body

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import mega.privacy.android.shared.theme.MegaAppTheme
import mega.privacy.android.core.ui.preview.CombinedThemePreviews
import mega.privacy.android.feature.devicecenter.ui.bottomsheet.tiles.InfoBottomSheetTile

/**
 * Test Tag for the Backup Folder Bottom Sheet Body
 */
internal const val BOTTOM_SHEET_BODY_BACKUP_FOLDER =
    "backup_folder_bottom_sheet_body:column_options_list"

/**
 * A [Composable] Bottom Sheet Body that displays options specific to a Backup Device Folder
 *
 * @param onInfoClicked Lambda that is executed when the "Info" Tile is selected
 */
@Composable
internal fun BackupFolderBottomSheetBody(
    onInfoClicked: () -> Unit,
) {
    Column(modifier = Modifier.testTag(BOTTOM_SHEET_BODY_BACKUP_FOLDER)) {
        InfoBottomSheetTile(onActionClicked = onInfoClicked, addSeparator = false)
    }
}

/**
 * A Preview Composable that displays the Bottom Sheet Bottom and its Options for a Backup Folder
 */
@CombinedThemePreviews
@Composable
private fun PreviewBackupFolderBottomSheet() {
    MegaAppTheme(isDark = isSystemInDarkTheme()) {
        BackupFolderBottomSheetBody(
            onInfoClicked = {},
        )
    }
}