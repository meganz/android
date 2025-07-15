package mega.privacy.android.feature.sync.navigation

import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import kotlinx.serialization.Serializable
import mega.privacy.android.core.ui.mapper.FileTypeIconMapper
import mega.privacy.android.feature.sync.ui.megapicker.MegaPickerRoute
import mega.privacy.android.feature.sync.ui.permissions.SyncPermissionsManager

/**
 * Route to the Stop Backup MEGA folder picker screen
 */
@Serializable
data object StopBackupMegaPicker

internal fun NavGraphBuilder.stopBackupMegaPickerDestination(
    syncPermissionsManager: SyncPermissionsManager,
    fileTypeIconMapper: FileTypeIconMapper,
    onBackPressed: () -> Unit,
) {
    composable<StopBackupMegaPicker> {
        MegaPickerRoute(
            hiltViewModel(),
            syncPermissionsManager,
            folderSelected = onBackPressed,
            backClicked = onBackPressed,
            fileTypeIconMapper = fileTypeIconMapper,
            isStopBackupMegaPicker = true,
        )
    }
}