package mega.privacy.android.feature.sync.navigation

import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import androidx.navigation.toRoute
import kotlinx.serialization.Serializable
import mega.privacy.android.core.nodecomponents.mapper.FileTypeIconMapper
import mega.privacy.android.feature.sync.ui.megapicker.MegaPickerRoute
import mega.privacy.android.feature.sync.ui.megapicker.MegaPickerViewModel
import mega.privacy.android.feature.sync.ui.permissions.SyncPermissionsManager

/**
 * Route to the Stop Backup MEGA folder picker screen
 */
@Serializable
internal data class StopBackupMegaPicker(val folderName: String?)

internal fun NavGraphBuilder.stopBackupMegaPickerDestination(
    syncPermissionsManager: SyncPermissionsManager,
    fileTypeIconMapper: FileTypeIconMapper,
    onBackPressed: () -> Unit,
) {
    composable<StopBackupMegaPicker> { navBackStackEntry ->
        val routeArg = navBackStackEntry.toRoute<StopBackupMegaPicker>()
        val viewModel =
            hiltViewModel<MegaPickerViewModel, MegaPickerViewModel.MegaPickerViewModelFactory> { factory ->
                factory.create(isStopBackup = true, folderName = routeArg.folderName)
            }
        MegaPickerRoute(
            viewModel = viewModel,
            syncPermissionsManager,
            folderSelected = onBackPressed,
            backClicked = onBackPressed,
            fileTypeIconMapper = fileTypeIconMapper,
            isStopBackupMegaPicker = true,
        )
    }
}
