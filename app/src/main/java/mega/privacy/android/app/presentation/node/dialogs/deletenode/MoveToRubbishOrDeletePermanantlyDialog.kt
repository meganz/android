package mega.privacy.android.app.presentation.node.dialogs.deletenode

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material.SnackbarHostState
import androidx.compose.material.rememberScaffoldState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import de.palm.composestateevents.EventEffect
import mega.privacy.android.app.R
import mega.privacy.android.core.ui.controls.dialogs.MegaAlertDialog
import mega.privacy.android.core.ui.preview.BooleanProvider
import mega.privacy.android.core.ui.preview.CombinedTextAndThemePreviews
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.shared.theme.MegaAppTheme


/**
 * Dialog to Move node to rubbish or delete a node
 * @param nodesList List of nodes
 * @param isNodeInRubbish is current node is in rubbish
 * @param modifier [Modifier]
 * @param onDismiss dismiss callback of dialog
 * @param snackBarHostState [SnackbarHostState]
 * @param viewModel [MoveToRubbishOrDeleteNodeDialog]
 */
@Composable
fun MoveToRubbishOrDeleteNodeDialog(
    modifier: Modifier = Modifier,
    nodesList: List<Long>,
    isNodeInRubbish: Boolean = false,
    onDismiss: () -> Unit,
    viewModel: MoveToRubbishOrDeleteNodeDialogViewModel,
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

    MegaAppTheme(isDark = isSystemInDarkTheme()) {
        MegaAlertDialog(
            modifier = modifier,
            text = message,
            confirmButtonText = positiveText,
            cancelButtonText = stringResource(id = R.string.general_cancel),
            onConfirm = {
                if (isNodeInRubbish) {
                    viewModel.deleteNodes(nodesList)
                } else {
                    viewModel.moveNodesToRubbishBin(nodesList)
                }
                onDismiss()
            },
            onDismiss = {
                onDismiss()
            }
        )
    }
}

@CombinedTextAndThemePreviews
@Composable
private fun PreviewMoveToRubbishOrDeleteNodeDialog(
    @PreviewParameter(BooleanProvider::class) isNodeInRubbish: Boolean,
) {
    MegaAppTheme(isDark = isSystemInDarkTheme()) {
        MoveToRubbishOrDeleteNodeDialog(
            nodesList = listOf(1),
            isNodeInRubbish = isNodeInRubbish,
            onDismiss = {},
            viewModel = hiltViewModel()
        )
    }
}