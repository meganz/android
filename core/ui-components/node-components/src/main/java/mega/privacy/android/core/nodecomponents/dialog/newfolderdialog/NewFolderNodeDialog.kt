package mega.privacy.android.core.nodecomponents.dialog.newfolderdialog

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import de.palm.composestateevents.EventEffect
import mega.android.core.ui.components.dialogs.BasicInputDialog
import mega.privacy.android.core.nodecomponents.R
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.exception.EmptyNodeNameException
import mega.privacy.android.domain.exception.InvalidNodeNameException
import mega.privacy.android.domain.exception.NodeNameAlreadyExistsException
import mega.privacy.android.shared.resources.R as sharedR

/**
 * Composable function to show a new folder creation dialog.
 *
 * @param viewModel The ViewModel for the dialog
 * @param parentNode The parent node ID where the folder will be created
 * @param modifier Modifier to be applied to the dialog
 * @param onDismiss Callback when dialog is dismissed
 */
@Composable
fun NewFolderNodeDialog(
    parentNode: NodeId,
    onCreateFolder: (NodeId?) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: NewFolderNodeDialogViewModel = hiltViewModel(),
    onDismiss: () -> Unit = {},
) {
    val context = LocalContext.current
    val dialogState by viewModel.uiState.collectAsStateWithLifecycle()
    var folderName by rememberSaveable { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    EventEffect(
        event = dialogState.folderCreatedEvent,
        onConsumed = viewModel::clearFolderCreatedEvent,
    ) {
        onCreateFolder(it)
    }

    EventEffect(
        event = dialogState.errorEvent,
        onConsumed = viewModel::clearError
    ) {
        errorMessage = when (it) {
            is EmptyNodeNameException -> context.getString(R.string.invalid_string)
            is InvalidNodeNameException -> context.getString(
                R.string.invalid_characters_defined, INVALID_CHARACTERS
            )

            is NodeNameAlreadyExistsException -> context.getString(R.string.same_item_name_warning)
            else -> null
        }
    }

    BasicInputDialog(
        title = stringResource(id = R.string.menu_new_folder),
        modifier = modifier,
        inputLabel = stringResource(id = R.string.context_new_folder_name),
        inputValue = folderName,
        onValueChange = { newValue ->
            folderName = newValue
            errorMessage = null
        },
        errorText = errorMessage,
        positiveButtonText = stringResource(id = R.string.general_create),
        onPositiveButtonClicked = {
            viewModel.createFolder(
                folderName = folderName,
                parentNodeId = parentNode,
            )
        },
        negativeButtonText = stringResource(id = sharedR.string.general_dialog_cancel_button),
        onNegativeButtonClicked = onDismiss,
        keyboardType = KeyboardType.Text,
        onDismiss = onDismiss,
    )
}