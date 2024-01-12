package mega.privacy.android.app.presentation.node.dialogs.changeextension

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import mega.privacy.android.app.R
import mega.privacy.android.core.ui.controls.dialogs.MegaAlertDialog
import mega.privacy.android.core.ui.preview.CombinedTextAndThemePreviews
import mega.privacy.android.shared.theme.MegaAppTheme

@Composable
internal fun ChangeNodeExtensionDialog(
    modifier: Modifier = Modifier,
    newNodeName: String,
    nodeId: Long,
    onDismiss: () -> Unit,
    viewModel: ChangeNodeExtensionDialogViewModel,
) {

    MegaAppTheme(isDark = isSystemInDarkTheme()) {

        ChangeNodeExtensionDialogBody(
            modifier = modifier,
            onChangeNodeExtension = {
                viewModel.handleAction(
                    ChangeNodeExtensionAction.OnChangeExtensionConfirmed(
                        nodeId,
                        newNodeName
                    )
                )
                onDismiss()
            },
            onDismiss = {
                onDismiss()
            }
        )
    }
}

@Composable
private fun ChangeNodeExtensionDialogBody(
    modifier: Modifier = Modifier,
    onChangeNodeExtension: () -> Unit,
    onDismiss: () -> Unit,
) {
    MegaAlertDialog(
        modifier = modifier,
        text = stringResource(id = R.string.file_extension_change_title),
        confirmButtonText = stringResource(id = R.string.action_change_anyway),
        cancelButtonText = stringResource(id = R.string.general_cancel),
        onConfirm = {
            onChangeNodeExtension()
            onDismiss()
        },
        onDismiss = {
            onDismiss()
        }
    )
}

@CombinedTextAndThemePreviews
@Composable
private fun MoveToRubbishOrDeleteNodeDialogPreview() {
    MegaAppTheme(isDark = isSystemInDarkTheme()) {
        ChangeNodeExtensionDialogBody(
            onChangeNodeExtension = {},
            onDismiss = {}
        )
    }
}