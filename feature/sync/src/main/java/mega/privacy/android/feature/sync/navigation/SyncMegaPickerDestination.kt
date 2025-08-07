package mega.privacy.android.feature.sync.navigation

import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import kotlinx.serialization.Serializable
import mega.privacy.android.core.nodecomponents.mapper.FileTypeIconMapper
import mega.privacy.android.feature.sync.ui.megapicker.MegaPickerRoute
import mega.privacy.android.feature.sync.ui.megapicker.MegaPickerViewModel
import mega.privacy.android.feature.sync.ui.permissions.SyncPermissionsManager

/**
 * Route to the MEGA folder picker screen
 */
@Serializable
internal data object SyncMegaPicker

internal fun NavGraphBuilder.syncMegaPickerDestination(
    syncPermissionsManager: SyncPermissionsManager,
    fileTypeIconMapper: FileTypeIconMapper,
    onNavigateBack: () -> Unit,
) {
    composable<SyncMegaPicker> {
        val viewModel =
            hiltViewModel<MegaPickerViewModel, MegaPickerViewModel.MegaPickerViewModelFactory> { factory ->
                factory.create(isStopBackup = false, folderName = "")
            }
        MegaPickerRoute(
            viewModel = viewModel,
            syncPermissionsManager,
            folderSelected = onNavigateBack,
            backClicked = onNavigateBack,
            fileTypeIconMapper = fileTypeIconMapper,
        )
    }
}
