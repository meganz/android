package mega.privacy.android.app.presentation.zipbrowser.view

import androidx.activity.compose.LocalOnBackPressedDispatcherOwner
import androidx.compose.runtime.Composable
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import mega.privacy.android.app.presentation.zipbrowser.ZipBrowserViewModel
import mega.privacy.android.domain.entity.zipbrowser.ZipEntryType

@Composable
internal fun ZipBrowserScreen(
    viewModel: ZipBrowserViewModel,
) {

    val uiState = viewModel.uiState.collectAsStateWithLifecycle().value
    val onBackPressedDispatcher = LocalOnBackPressedDispatcherOwner.current?.onBackPressedDispatcher

    ZipBrowserView(
        items = uiState.items,
        parentFolderName = uiState.parentFolderName,
        folderDepth = uiState.folderDepth,
        onItemClicked = { uiEntity ->
            if (uiEntity.zipEntryType == ZipEntryType.Folder) {
                viewModel.openFolder(uiEntity.path)
            }
        },
        onBackPressed = {
            if (uiState.folderDepth == 0) {
                onBackPressedDispatcher?.onBackPressed()
            } else {
                viewModel.handleOnBackPressed()
            }
        }
    )
}