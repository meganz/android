package mega.privacy.android.core.nodecomponents.dialog.textfile

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.hilt.navigation.compose.hiltViewModel
import kotlinx.coroutines.launch
import mega.android.core.ui.components.dialogs.BasicInputDialog
import mega.privacy.android.core.nodecomponents.dialog.newfolderdialog.INVALID_CHARACTERS
import mega.privacy.android.core.nodecomponents.model.NodeSourceTypeInt
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.entity.texteditor.TextEditorMode
import mega.privacy.android.domain.exception.DotNameException
import mega.privacy.android.domain.exception.DoubleDotNameException
import mega.privacy.android.domain.exception.EmptyNodeNameException
import mega.privacy.android.domain.exception.InvalidNodeNameException
import mega.privacy.android.domain.exception.NodeNameAlreadyExistsException
import mega.privacy.android.domain.exception.NodeNameException
import mega.privacy.android.navigation.OpenTextEditorParams
import mega.privacy.android.navigation.extensions.rememberMegaNavigator
import mega.privacy.android.shared.nodes.R as NodesR
import mega.privacy.android.shared.resources.R as sharedR

/**
 * Composable function to show a new text file creation dialog.
 *
 * @param parentNode The parent node ID where the text file will be created
 * @param modifier Modifier to be applied to the dialog
 * @param viewModel The ViewModel for the dialog
 * @param onDismiss Callback when dialog is dismissed
 */
@Composable
fun NewTextFileNodeDialog(
    parentNode: NodeId,
    modifier: Modifier = Modifier,
    viewModel: NewTextFileNodeDialogViewModel = hiltViewModel(),
    onDismiss: () -> Unit = {},
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val megaNavigator = rememberMegaNavigator()
    var fileName by rememberSaveable(stateSaver = TextFieldValue.Saver) {
        mutableStateOf(value = TextFieldValue(".txt", TextRange(0)))
    }
    var errorMessage by remember { mutableStateOf<NodeNameException?>(null) }

    BasicInputDialog(
        title = stringResource(id = sharedR.string.general_new_text_file),
        modifier = modifier.testTag(NEW_TEXT_FILE_NODE_DIALOG_TAG),
        inputValue = fileName,
        onValueChange = { newValue ->
            fileName = newValue
            errorMessage = null
        },
        isAutoShowKeyboard = true,
        errorText = errorMessage?.text(),
        positiveButtonText = stringResource(id = sharedR.string.general_create_label),
        onPositiveButtonClicked = {
            coroutineScope.launch {
                viewModel.createTextFile(
                    fileName = fileName.text.trim(),
                    parentNodeId = parentNode,
                ).onSuccess { (parentNode, fileName) ->
                    megaNavigator.openTextEditor(
                        context = context,
                        params = OpenTextEditorParams.CloudNode(
                            nodeId = parentNode,
                            nodeSourceType = NodeSourceTypeInt.FILE_BROWSER_ADAPTER,
                            mode = TextEditorMode.Create,
                            fileName = fileName,
                        ),
                    )
                    onDismiss()
                }.onFailure {
                    errorMessage = it as? NodeNameException
                }
            }
        },
        negativeButtonText = stringResource(id = sharedR.string.general_dialog_cancel_button),
        onNegativeButtonClicked = onDismiss,
        keyboardType = KeyboardType.Text,
        capitalization = KeyboardCapitalization.None,
        onDismiss = onDismiss,
        inputTextAlign = TextAlign.End
    )
}

@Composable
private fun NodeNameException.text(): String = when (this) {
    is EmptyNodeNameException -> stringResource(NodesR.string.invalid_string)
    is DotNameException -> stringResource(sharedR.string.general_invalid_dot_name_warning)
    is DoubleDotNameException -> stringResource(sharedR.string.general_invalid_double_dot_name_warning)
    is InvalidNodeNameException -> stringResource(
        sharedR.string.general_invalid_characters_defined, INVALID_CHARACTERS
    )

    is NodeNameAlreadyExistsException -> stringResource(NodesR.string.same_file_name_warning)
}

internal const val NEW_TEXT_FILE_NODE_DIALOG_TAG = "new_text_file_node:dialog"
