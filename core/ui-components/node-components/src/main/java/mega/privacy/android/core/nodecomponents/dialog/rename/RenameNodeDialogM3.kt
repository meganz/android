package mega.privacy.android.core.nodecomponents.dialog.rename

import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import de.palm.composestateevents.EventEffect
import mega.android.core.ui.components.dialogs.BasicDialog
import mega.android.core.ui.components.dialogs.BasicInputDialog
import mega.android.core.ui.preview.CombinedThemePreviews
import mega.android.core.ui.theme.AndroidThemeForPreviews
import mega.privacy.android.core.nodecomponents.R
import mega.privacy.android.core.nodecomponents.dialog.rename.RenameNodeDialogAction.OnChangeNodeExtensionDialogShown
import mega.privacy.android.core.nodecomponents.dialog.rename.RenameNodeDialogAction.OnLoadNodeName
import mega.privacy.android.core.nodecomponents.dialog.rename.RenameNodeDialogAction.OnRenameConfirmed
import mega.privacy.android.core.nodecomponents.dialog.rename.RenameNodeDialogAction.OnRenameValidationPassed
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.shared.resources.R as sharedR

internal const val RENAME_NODE_DIALOG_TAG = "rename_node_dialog:input_dialog"
internal const val RENAME_NODE_DIALOG_CONFIRMATION_DIALOG =
    "rename_node_dialog:rename_confirmation_dialog"
internal const val NODE_NAME_INVALID_CHARACTERS = "\" * / : < > ? \\ |"

/**
 * A Composable Dialog that allows the User to rename a Node
 * This dialog handles both regular renames and extension change confirmations internally.
 *
 * @param nodeId The Node ID of the Node to rename
 * @param onDismiss Lambda that is triggered when the dialog is dismissed
 */
@Composable
fun RenameNodeDialogM3(
    nodeId: NodeId,
    onDismiss: () -> Unit,
    viewModel: RenameNodeDialogViewModel = hiltViewModel(),
) {
    val uiState by viewModel.state.collectAsStateWithLifecycle()

    RenameNodeDialogM3View(
        uiState = uiState,
        onLoadNodeName = {
            viewModel.handleAction(OnLoadNodeName(nodeId.longValue))
        },
        resetRenameValidationPassed = {
            viewModel.handleAction(OnRenameValidationPassed)
        },
        resetShowChangeNodeExtensionDialog = {
            viewModel.handleAction(OnChangeNodeExtensionDialogShown)
        },
        onRenameConfirmed = { newNodeName ->
            viewModel.handleAction(OnRenameConfirmed(nodeId.longValue, newNodeName))
        },
        onDismiss = onDismiss,
        onRenameNode = { newNodeName ->
            viewModel.renameNode(nodeId, newNodeName)
            onDismiss()
        }
    )
}

@Composable
internal fun RenameNodeDialogM3View(
    uiState: RenameNodeDialogState,
    onLoadNodeName: () -> Unit,
    resetRenameValidationPassed: () -> Unit,
    resetShowChangeNodeExtensionDialog: () -> Unit,
    onRenameConfirmed: (String) -> Unit,
    onDismiss: () -> Unit,
    onRenameNode: (String) -> Unit,
) {
    var showExtensionConfirmation by remember { mutableStateOf(false) }
    var pendingNewName by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        onLoadNodeName()
    }

    EventEffect(
        event = uiState.renameValidationPassedEvent,
        onConsumed = resetRenameValidationPassed,
        action = onDismiss
    )

    EventEffect(
        event = uiState.showChangeNodeExtensionDialogEvent,
        onConsumed = resetShowChangeNodeExtensionDialog,
        action = {
            showExtensionConfirmation = true
            pendingNewName = it
        }
    )

    when {
        showExtensionConfirmation && pendingNewName != null -> {
            RenameConfirmationDialog(
                newNodeName = pendingNewName.orEmpty(),
                onDismiss = {
                    showExtensionConfirmation = false
                    pendingNewName = null
                    onDismiss()
                },
                onChangeNodeExtension = { newName ->
                    onRenameNode(newName)
                    showExtensionConfirmation = false
                    pendingNewName = null
                    onDismiss()
                }
            )
        }

        else -> {
            RenameNodeDialogBody(
                nodeName = uiState.nodeName.orEmpty(),
                errorMessage = uiState.errorMessage,
                onRenameConfirmed = { newNodeName ->
                    onRenameConfirmed(newNodeName)
                },
                onRenameCancelled = onDismiss
            )
        }
    }
}

@Composable
private fun RenameConfirmationDialog(
    newNodeName: String,
    onDismiss: () -> Unit,
    onChangeNodeExtension: (newName: String) -> Unit,
) {
    BasicDialog(
        modifier = Modifier.testTag(RENAME_NODE_DIALOG_CONFIRMATION_DIALOG),
        title = stringResource(id = R.string.file_extension_change_title),
        description = stringResource(id = R.string.file_extension_change_warning),
        positiveButtonText = stringResource(id = R.string.action_change_anyway),
        negativeButtonText = stringResource(id = sharedR.string.general_dialog_cancel_button),
        onPositiveButtonClicked = {
            onChangeNodeExtension(newNodeName)
        },
        onNegativeButtonClicked = onDismiss,
        onDismiss = onDismiss,
    )
}

@Composable
private fun RenameNodeDialogBody(
    nodeName: String,
    @StringRes errorMessage: Int?,
    onRenameConfirmed: (String) -> Unit,
    onRenameCancelled: () -> Unit,
) {
    var inputValue by rememberSaveable(
        inputs = arrayOf(nodeName),
        stateSaver = TextFieldValue.Saver
    ) {
        mutableStateOf(value = TextFieldValue(nodeName, TextRange(nodeName.length)))
    }

    LaunchedEffect(nodeName) {
        val dotIndex = nodeName.lastIndexOf('.')
        val cursorIndex = if (dotIndex != -1) dotIndex else nodeName.length
        inputValue = TextFieldValue(text = nodeName, selection = TextRange(0, cursorIndex))
    }

    BasicInputDialog(
        modifier = Modifier.testTag(RENAME_NODE_DIALOG_TAG),
        title = stringResource(id = sharedR.string.context_rename),
        positiveButtonText = stringResource(id = sharedR.string.context_rename),
        negativeButtonText = stringResource(id = sharedR.string.general_dialog_cancel_button),
        onValueChange = { inputValue = it },
        errorText = errorMessage?.let { nonNullErrorMessage ->
            if (nonNullErrorMessage == R.string.invalid_characters_defined) {
                stringResource(nonNullErrorMessage, NODE_NAME_INVALID_CHARACTERS)
            } else {
                stringResource(nonNullErrorMessage)
            }
        },
        inputValue = inputValue,
        onPositiveButtonClicked = {
            onRenameConfirmed(inputValue.text)
        },
        onNegativeButtonClicked = onRenameCancelled,
        isAutoShowKeyboard = true,
        onDismiss = onRenameCancelled
    )
}

@Composable
@CombinedThemePreviews
private fun PreviewRenameNodeDialogM3ViewNormal() {
    AndroidThemeForPreviews {
        RenameNodeDialogM3View(
            uiState = RenameNodeDialogState(
                nodeName = "document.pdf"
            ),
            onLoadNodeName = {},
            resetRenameValidationPassed = {},
            resetShowChangeNodeExtensionDialog = {},
            onRenameConfirmed = {},
            onDismiss = {},
            onRenameNode = {}
        )
    }
}

@Composable
@CombinedThemePreviews
private fun PreviewRenameNodeDialogM3ViewError() {
    AndroidThemeForPreviews {
        RenameNodeDialogM3View(
            uiState = RenameNodeDialogState(
                nodeName = "document.pdf",
                errorMessage = R.string.invalid_characters_defined
            ),
            onLoadNodeName = {},
            resetRenameValidationPassed = {},
            resetShowChangeNodeExtensionDialog = {},
            onRenameConfirmed = {},
            onDismiss = {},
            onRenameNode = {}
        )
    }
}

@Composable
@CombinedThemePreviews
private fun PreviewRenameNodeDialogM3ViewEmpty() {
    AndroidThemeForPreviews {
        RenameNodeDialogM3View(
            uiState = RenameNodeDialogState(
                nodeName = null
            ),
            onLoadNodeName = {},
            resetRenameValidationPassed = {},
            resetShowChangeNodeExtensionDialog = {},
            onRenameConfirmed = {},
            onDismiss = {},
            onRenameNode = {}
        )
    }
}

@Composable
@CombinedThemePreviews
private fun PreviewRenameNodeDialogM3ViewFolder() {
    AndroidThemeForPreviews {
        RenameNodeDialogM3View(
            uiState = RenameNodeDialogState(
                nodeName = "My Documents"
            ),
            onLoadNodeName = {},
            resetRenameValidationPassed = {},
            resetShowChangeNodeExtensionDialog = {},
            onRenameConfirmed = {},
            onDismiss = {},
            onRenameNode = {}
        )
    }
}
