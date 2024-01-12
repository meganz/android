package mega.privacy.android.app.presentation.node.dialogs.renamenode

import androidx.annotation.StringRes
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import de.palm.composestateevents.EventEffect
import mega.privacy.android.app.R
import mega.privacy.android.core.ui.preview.CombinedThemePreviews
import mega.privacy.android.legacy.core.ui.controls.dialogs.InputDialog
import mega.privacy.android.shared.theme.MegaAppTheme
import mega.privacy.android.app.presentation.node.dialogs.renamenode.RenameNodeDialogAction.OnLoadNodeName
import mega.privacy.android.app.presentation.node.dialogs.renamenode.RenameNodeDialogAction.OnRenameConfirmed
import mega.privacy.android.app.presentation.node.dialogs.renamenode.RenameNodeDialogAction.OnRenameValidationPassed
import mega.privacy.android.app.presentation.node.dialogs.renamenode.RenameNodeDialogAction.OnChangeNodeExtensionDialogShown

internal const val RENAME_NODE_DIALOG_TAG = "rename_node_dialog:input_dialog"
internal const val NODE_NAME_INVALID_CHARACTERS = "\" * / : < > ? \\ |"

/**
 * A Composable Dialog that allows the User to rename a Node
 *
 * @param nodeId The Node ID of the Node to rename
 * @param onDismiss Lambda that is triggered when the dialog is dismissed
 * @param onOpenChangeExtensionDialog Lambda that is triggered when the User attempts to change the file extensions
 */
@Composable
internal fun RenameNodeDialog(
    nodeId: Long,
    onDismiss: () -> Unit,
    onOpenChangeExtensionDialog: (newName: String) -> Unit,
    viewModel: RenameNodeDialogViewModel,
) {
    val uiState by viewModel.state.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        viewModel.handleAction(OnLoadNodeName(nodeId))
    }

    EventEffect(
        event = uiState.renameValidationPassedEvent,
        onConsumed = {
            viewModel.handleAction(OnRenameValidationPassed)
        },
        action = {
            onDismiss()
        }
    )

    EventEffect(event = uiState.showChangeNodeExtensionDialogEvent, onConsumed = {
        viewModel.handleAction(OnChangeNodeExtensionDialogShown)
    }, action = { newName ->
        onDismiss()
        onOpenChangeExtensionDialog(newName)
    })

    uiState.nodeName?.let { nodeName ->
        RenameNodeDialogBody(
            nodeName = nodeName,
            errorMessage = uiState.errorMessage,
            onRenameConfirmed = { newNodeName ->
                viewModel.handleAction(OnRenameConfirmed(nodeId, newNodeName))
            },
            onRenameCancelled = {
                onDismiss()
            },
        )
    }
}

@Composable
private fun RenameNodeDialogBody(
    nodeName: String,
    @StringRes errorMessage: Int?,
    onRenameConfirmed: (String) -> Unit,
    onRenameCancelled: () -> Unit,
) {
    var initialInput by rememberSaveable { mutableStateOf(nodeName) }

    MegaAppTheme(isDark = isSystemInDarkTheme()) {
        InputDialog(
            modifier = Modifier.testTag(RENAME_NODE_DIALOG_TAG),
            title = stringResource(id = R.string.context_rename),
            confirmButtonText = stringResource(id = R.string.context_rename),
            cancelButtonText = stringResource(id = R.string.general_cancel),
            text = initialInput,
            onInputChange = { initialInput = it },
            error = errorMessage?.let { nonNullErrorMessage ->
                if (nonNullErrorMessage == R.string.invalid_characters_defined) {
                    stringResource(nonNullErrorMessage, NODE_NAME_INVALID_CHARACTERS)
                } else {
                    stringResource(nonNullErrorMessage)
                }
            },
            onConfirm = onRenameConfirmed,
            onDismiss = onRenameCancelled,
        )
    }
}

@CombinedThemePreviews
@Composable
private fun PreviewRenameDeviceDialogBody(
    @PreviewParameter(RenameNodeDialogBodyPreviewProvider::class) renameDialogState: RenameNodeDialogState,
) {
    MegaAppTheme(isDark = isSystemInDarkTheme()) {
        RenameNodeDialogBody(
            nodeName = renameDialogState.nodeName ?: "",
            errorMessage = renameDialogState.errorMessage,
            onRenameConfirmed = {},
            onRenameCancelled = {},
        )
    }
}

private class RenameNodeDialogBodyPreviewProvider :
    PreviewParameterProvider<RenameNodeDialogState> {
    override val values: Sequence<RenameNodeDialogState>
        get() = sequenceOf(
            RenameNodeDialogState(nodeName = "Camera Uploads"),
            RenameNodeDialogState(nodeName = "Test", errorMessage = R.string.invalid_characters)
        )
}
