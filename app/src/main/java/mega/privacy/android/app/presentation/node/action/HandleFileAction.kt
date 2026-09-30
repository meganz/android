package mega.privacy.android.app.presentation.node.action

import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import mega.android.core.ui.extensions.showAutoDurationSnackbar
import mega.privacy.android.app.presentation.node.NodeActionsViewModel
import mega.privacy.android.core.nodecomponents.action.openOtherFile
import mega.privacy.android.navigation.megaNavigator
import java.io.File

/**
 * Handle node action click
 */
@Composable
fun HandleFileAction(
    file: File,
    isOpenWith: Boolean,
    snackBarHostState: SnackbarHostState?,
    onActionHandled: () -> Unit,
    coroutineScope: CoroutineScope,
) {
    val nodeActionsViewModel: NodeActionsViewModel = hiltViewModel()
    val context = LocalContext.current
    val megaNavigator = remember { context.megaNavigator }

    LaunchedEffect(file) {
        openOtherFile(
            onOpenZipBrowser = { zipFilePath, nodeHandle, onError ->
                megaNavigator.openZipBrowserActivity(
                    context = context,
                    zipFilePath = zipFilePath,
                    nodeHandle = nodeHandle,
                    onError = onError
                )
            },
            file = file,
            typedFileNode = null,
            isOpenWith = isOpenWith,
            fileTypeInfo = nodeActionsViewModel.getTypeInfo(file),
            applyNodeContentUri = nodeActionsViewModel::applyNodeContentUri,
            showSnackbar = { message ->
                coroutineScope.launch {
                    snackBarHostState?.showAutoDurationSnackbar(message)
                }
            },
            context = context,
        )

        onActionHandled()
    }
}
