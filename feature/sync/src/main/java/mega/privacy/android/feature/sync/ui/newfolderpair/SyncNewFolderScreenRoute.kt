package mega.privacy.android.feature.sync.ui.newfolderpair

import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import mega.privacy.android.feature.sync.ui.newfolderpair.SyncNewFolderAction.LocalFolderSelected
import mega.privacy.android.feature.sync.ui.newfolderpair.SyncNewFolderAction.FolderNameChanged
import mega.privacy.android.feature.sync.ui.newfolderpair.SyncNewFolderAction.NextClicked
import mega.privacy.android.feature.sync.ui.permissions.SyncPermissionsManager

@Composable
internal fun SyncNewFolderScreenRoute(
    viewModel: SyncNewFolderViewModel,
    syncPermissionsManager: SyncPermissionsManager,
    openSelectMegaFolderScreen: () -> Unit,
    openNextScreen: (SyncNewFolderState) -> Unit,
    onBackClicked: () -> Unit,
) {
    val state = viewModel.state.collectAsStateWithLifecycle()

    val context = LocalContext.current

    var showAllFilesAccessBanner by remember {
        mutableStateOf(!syncPermissionsManager.isManageExternalStoragePermissionGranted())
    }
    var showDisableBatteryOptimizationsBanner by remember {
        mutableStateOf(
            !showAllFilesAccessBanner && !syncPermissionsManager.isDisableBatteryOptimizationGranted()
        )
    }

    val permissionsLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { _ ->
        showAllFilesAccessBanner =
            !syncPermissionsManager.isManageExternalStoragePermissionGranted()
        showDisableBatteryOptimizationsBanner =
            !showAllFilesAccessBanner &&
                    !syncPermissionsManager.isDisableBatteryOptimizationGranted()
    }

    SyncNewFolderScreen(
        folderPairName = state.value.folderPairName,
        selectedLocalFolder = state.value.selectedLocalFolder,
        selectedMegaFolder = state.value.selectedMegaFolder,
        localFolderSelected = { viewModel.handleAction(LocalFolderSelected(it)) },
        folderNameChanged = { viewModel.handleAction(FolderNameChanged(it)) },
        selectMegaFolderClicked = openSelectMegaFolderScreen,
        syncClicked = {
            viewModel.handleAction(NextClicked)
            openNextScreen(state.value)
        },
        syncPermissionsManager = syncPermissionsManager
    )

    val onBack = {
        onBackClicked()
    }

    BackHandler(onBack = onBack)
}