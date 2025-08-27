package mega.privacy.android.app.presentation.node.dialogs.deletenode

import mega.privacy.android.shared.resources.R as sharedR
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.hilt.navigation.compose.hiltViewModel
import mega.privacy.android.app.R
import mega.privacy.android.core.nodecomponents.dialog.delete.MoveToRubbishOrDeleteNodeDialogViewModel
import mega.privacy.android.shared.original.core.ui.controls.dialogs.MegaAlertDialog
import mega.privacy.android.shared.original.core.ui.preview.BooleanProvider
import mega.privacy.android.shared.original.core.ui.preview.CombinedTextAndThemePreviews
import mega.privacy.android.shared.original.core.ui.theme.OriginalTheme


/**
 * Dialog to Move node to rubbish or delete a node
 * @param modifier [Modifier]
 * @param nodesList List of nodes
 * @param isNodeInRubbish is current node is in rubbish
 * @param onDismiss dismiss callback of dialog
 * @param viewModel [MoveToRubbishOrDeleteNodeDialog]
 */
@Composable
fun MoveToRubbishOrDeleteNodeDialog(
    nodesList: List<Long>,
    isNodeInRubbish: Boolean = false,
    viewModel: MoveToRubbishOrDeleteNodeDialogViewModel = hiltViewModel(),
    onDismiss: () -> Unit,
) {
    val message = when {
        isNodeInRubbish -> stringResource(id = R.string.confirmation_delete_from_mega)
        else -> stringResource(id = R.string.confirmation_move_to_rubbish)
    }
    val positiveText = if (isNodeInRubbish) {
        stringResource(id = R.string.rubbish_bin_delete_confirmation_dialog_button_delete)
    } else {
        stringResource(id = R.string.general_move)
    }

    MoveToRubbishOrDeleteNodeDialogBody(
        message = message,
        positiveText = positiveText,
        onPositiveButtonClicked = {
            if (isNodeInRubbish) {
                viewModel.deleteNodes(nodesList)
            } else {
                viewModel.moveNodesToRubbishBin(nodesList)
            }
            onDismiss()
        },
        onNegativeButtonClicked = {
            onDismiss()
        },
    )
}

@Composable
private fun MoveToRubbishOrDeleteNodeDialogBody(
    message: String,
    positiveText: String,
    onPositiveButtonClicked: () -> Unit,
    onNegativeButtonClicked: () -> Unit,
) {
    OriginalTheme(isDark = isSystemInDarkTheme()) {
        MegaAlertDialog(
            text = message,
            confirmButtonText = positiveText,
            cancelButtonText = stringResource(id = sharedR.string.general_dialog_cancel_button),
            onConfirm = onPositiveButtonClicked,
            onDismiss = onNegativeButtonClicked
        )
    }
}

@CombinedTextAndThemePreviews
@Composable
private fun MoveToRubbishOrDeleteNodeDialogBodyPreview(
    @PreviewParameter(BooleanProvider::class) isNodeInRubbish: Boolean,
) {
    OriginalTheme(isDark = isSystemInDarkTheme()) {
        MoveToRubbishOrDeleteNodeDialogBody(
            message = stringResource(id = R.string.confirmation_delete_from_mega),
            positiveText = stringResource(id = R.string.rubbish_bin_delete_confirmation_dialog_button_delete),
            onPositiveButtonClicked = {},
            onNegativeButtonClicked = {}
        )
    }
}