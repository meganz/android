package mega.privacy.android.app.presentation.zipbrowser.view

import androidx.activity.compose.LocalOnBackPressedDispatcherOwner
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import mega.privacy.android.app.presentation.zipbrowser.ZipBrowserViewModel

@Composable
internal fun ZipBrowserScreen(
    modifier: Modifier,
    viewModel: ZipBrowserViewModel,
) {

    val uiState = viewModel.uiState.collectAsStateWithLifecycle().value
    val onBackPressedDispatcher = LocalOnBackPressedDispatcherOwner.current?.onBackPressedDispatcher

    ZipBrowserView(
        modifier = modifier,
        items = uiState.items,
        parentFolderName = uiState.parentFolderName,
        folderDepth = uiState.folderDepth,
        showProgressBar = uiState.showUnzipProgressBar,
        showAlertDialog = uiState.showAlertDialog,
        showSnackBar = uiState.showSnackBar,
        onItemClicked = viewModel::itemClicked,
        onBackPressed = {
            if (uiState.folderDepth == 0) {
                onBackPressedDispatcher?.onBackPressed()
            } else {
                viewModel.handleOnBackPressed()
            }
        },
        onDialogDismiss = { viewModel.updateShowAlertDialog(false) },
        onSnackBarShown = { viewModel.updateShowSnackBar(false) }
    )
}