package mega.privacy.android.core.nodecomponents.dialog.textfile

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.hilt.navigation.compose.hiltViewModel
import kotlinx.coroutines.launch
import mega.android.core.ui.components.MegaText
import mega.android.core.ui.components.dialogs.BasicInputDialog
import mega.android.core.ui.theme.values.TextColor
import mega.privacy.android.core.nodecomponents.R
import mega.privacy.android.core.nodecomponents.dialog.newfolderdialog.INVALID_CHARACTERS
import mega.privacy.android.core.nodecomponents.extension.rememberMegaNavigator
import mega.privacy.android.core.nodecomponents.model.NodeSourceTypeInt
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.entity.texteditor.TextEditorMode
import mega.privacy.android.domain.exception.EmptyNodeNameException
import mega.privacy.android.domain.exception.InvalidNodeNameException
import mega.privacy.android.domain.exception.NodeNameAlreadyExistsException
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
    var fileName by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    BasicInputDialog(
        title = stringResource(id = R.string.dialog_title_new_text_file),
        modifier = modifier,
        suffix = {
            MegaText(
                text = ".txt", // Suffix for the file name no need to be localized
                textColor = TextColor.Placeholder,
                style = MaterialTheme.typography.bodyLarge
            )
        },
        inputValue = fileName,
        onValueChange = { newValue ->
            fileName = newValue
            errorMessage = null
        },
        errorText = errorMessage,
        positiveButtonText = stringResource(id = R.string.general_create),
        onPositiveButtonClicked = {
            coroutineScope.launch {
                viewModel.createTextFile(
                    fileName = fileName.trim() + ".txt",
                    parentNodeId = parentNode,
                ).onSuccess { (parentNode, fileName) ->
                    megaNavigator.openTextEditorActivity(
                        context = context,
                        currentNodeId = parentNode,
                        fileName = fileName,
                        nodeSourceType = NodeSourceTypeInt.FILE_BROWSER_ADAPTER,
                        mode = TextEditorMode.Create
                    )
                    onDismiss()
                }.onFailure {
                    errorMessage = when (it) {
                        is EmptyNodeNameException -> context.getString(R.string.invalid_string)
                        is InvalidNodeNameException -> context.getString(
                            R.string.invalid_characters_defined, INVALID_CHARACTERS
                        )

                        is NodeNameAlreadyExistsException -> context.getString(R.string.same_file_name_warning)
                        else -> null
                    }
                }
            }
        },
        negativeButtonText = stringResource(id = sharedR.string.general_dialog_cancel_button),
        onNegativeButtonClicked = onDismiss,
        keyboardType = KeyboardType.Text,
        onDismiss = onDismiss,
        inputTextAlign = TextAlign.End
    )
} 