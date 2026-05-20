package mega.privacy.android.app.main.dialog.newfile

import androidx.compose.runtime.getValue
import androidx.compose.ui.res.stringResource
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
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
import mega.privacy.android.navigation.destination.NewURLFileDialogNavKey
import mega.privacy.android.shared.nodes.dialog.newfile.NewTextFileNodeDialog
import mega.privacy.android.shared.nodes.dialog.newfile.NewTextFileNodeDialogUiState.Companion.DEFAULT_LINK_FILE_EXTENSION
import mega.privacy.android.shared.nodes.dialog.newfile.NewTextFileNodeDialogUiState.Companion.DEFAULT_TEXT_FILE_EXTENSION
import mega.privacy.android.shared.nodes.dialog.newfile.NewTextFileNodeDialogViewModel
import mega.privacy.android.shared.nodes.model.NodeSourceTypeInt
import mega.privacy.android.shared.resources.R as sharedR

data object NewTextFileDialogDestinations : AppDialogDestinations {
    override val navigationGraph: EntryProviderScope<in DialogNavKey>.(NavigationHandler, () -> Unit) -> Unit =
        { navigationHandler, onHandled ->
            newTextFileDialogDestination(
                remove = navigationHandler::remove,
                navigate = navigationHandler::navigate,
                returnResult = navigationHandler::returnResult,
                onDialogHandled = onHandled,
            )
            newURLFileDialogDestination(
                remove = navigationHandler::remove,
                returnResult = navigationHandler::returnResult,
                onDialogHandled = onHandled,
            )
        }
}

fun EntryProviderScope<in DialogNavKey>.newTextFileDialogDestination(
    remove: (NavKey) -> Unit,
    navigate: (NavKey) -> Unit,
    returnResult: (String, String) -> Unit,
    onDialogHandled: () -> Unit,
) {
    entry<NewTextFileDialogNavKey>(
        metadata = DialogSceneStrategy.dialog()
    ) { key ->
        val viewModel =
            hiltViewModel<NewTextFileNodeDialogViewModel, NewTextFileNodeDialogViewModel.Factory>(
                creationCallback = {
                    it.create(
                        NewTextFileNodeDialogViewModel.Args(
                            parentNodeId = key.parentNodeId,
                            defaultExtension = DEFAULT_TEXT_FILE_EXTENSION
                        )
                    )
                }
            )
        val uiState by viewModel.uiState.collectAsStateWithLifecycle()

        NewTextFileNodeDialog(
            uiState = uiState,
            title = stringResource(id = sharedR.string.general_new_text_file),
            onConfirm = { fileName ->
                if (key.returnFileName) {
                    returnResult(NewTextFileDialogNavKey.FILE_NAME_RESULT, fileName)
                } else {
                    navigate(
                        LegacyTextEditorNavKey(
                            nodeHandle = key.parentNodeId.longValue,
                            mode = TextEditorMode.Create.value,
                            nodeSourceType = NodeSourceTypeInt.FILE_BROWSER_ADAPTER,
                            fileName = fileName,
                        )
                    )
                    remove(key)
                }
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

fun EntryProviderScope<in DialogNavKey>.newURLFileDialogDestination(
    remove: (NavKey) -> Unit,
    returnResult: (String, String) -> Unit,
    onDialogHandled: () -> Unit,
) {
    entry<NewURLFileDialogNavKey>(
        metadata = DialogSceneStrategy.dialog()
    ) { key ->
        val viewModel =
            hiltViewModel<NewTextFileNodeDialogViewModel, NewTextFileNodeDialogViewModel.Factory>(
                creationCallback = {
                    it.create(
                        NewTextFileNodeDialogViewModel.Args(
                            parentNodeId = key.parentNodeId,
                            defaultExtension = DEFAULT_LINK_FILE_EXTENSION,
                        )
                    )
                }
            )
        val uiState by viewModel.uiState.collectAsStateWithLifecycle()

        NewTextFileNodeDialog(
            uiState = uiState,
            title = stringResource(id = sharedR.string.general_new_link),
            onConfirm = { fileName ->
                returnResult(NewURLFileDialogNavKey.FILE_NAME_RESULT, fileName)
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
