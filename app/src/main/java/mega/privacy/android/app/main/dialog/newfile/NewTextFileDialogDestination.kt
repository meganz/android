package mega.privacy.android.app.main.dialog.newfile

import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.scene.DialogSceneStrategy
import mega.privacy.android.domain.entity.texteditor.TextEditorMode
import mega.privacy.android.navigation.contract.NavigationHandler
import mega.privacy.android.navigation.contract.dialog.AppDialogDestinations
import mega.privacy.android.navigation.contract.dialog.DialogNavKey
import mega.privacy.android.navigation.destination.LegacyTextEditorNavKey
import mega.privacy.android.navigation.destination.NewTextFileDialogNavKey
import mega.privacy.android.shared.nodes.dialog.newfile.NewTextFileNodeDialog
import mega.privacy.android.shared.nodes.dialog.newfile.NewTextFileNodeDialogViewModel
import mega.privacy.android.shared.nodes.model.NodeSourceTypeInt

data object NewTextFileDialogDestinations : AppDialogDestinations {
    override val navigationGraph: EntryProviderScope<in DialogNavKey>.(NavigationHandler, () -> Unit) -> Unit =
        { navigationHandler, onHandled ->
            newTextFileDialogDestination(
                remove = navigationHandler::remove,
                navigate = navigationHandler::navigate,
                onDialogHandled = onHandled,
            )
        }
}

fun EntryProviderScope<in DialogNavKey>.newTextFileDialogDestination(
    remove: (NavKey) -> Unit,
    navigate: (NavKey) -> Unit,
    onDialogHandled: () -> Unit,
) {
    entry<NewTextFileDialogNavKey>(
        metadata = DialogSceneStrategy.dialog()
    ) { key ->
        val viewModel =
            hiltViewModel<NewTextFileNodeDialogViewModel, NewTextFileNodeDialogViewModel.Factory>(
                creationCallback = { it.create(NewTextFileNodeDialogViewModel.Args(key.parentNodeId)) }
            )
        val uiState by viewModel.uiState.collectAsStateWithLifecycle()

        NewTextFileNodeDialog(
            uiState = uiState,
            onConfirm = { fileName ->
                navigate(
                    LegacyTextEditorNavKey(
                        nodeHandle = key.parentNodeId.longValue,
                        mode = TextEditorMode.Create.value,
                        nodeSourceType = NodeSourceTypeInt.FILE_BROWSER_ADAPTER,
                        fileName = fileName,
                    )
                )
                remove(key)
                onDialogHandled()
            },
            onFileNameChanged = viewModel::onFileNameChanged,
            validateFileName = viewModel::validateFileName,
            onValidationSuccessEventConsumed = viewModel::onValidationSuccessEventConsumed,
            onDismiss = {
                remove(key)
                onDialogHandled()
            },
        )
    }
}
