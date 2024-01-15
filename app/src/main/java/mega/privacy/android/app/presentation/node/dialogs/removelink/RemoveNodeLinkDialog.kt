package mega.privacy.android.app.presentation.node.dialogs.removelink

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import mega.privacy.android.app.R
import mega.privacy.android.core.ui.controls.dialogs.MegaAlertDialog
import mega.privacy.android.core.ui.preview.CombinedThemePreviews
import mega.privacy.android.shared.theme.MegaAppTheme

/**
 * Dialog to remove node link
 * @param modifier [Modifier]
 * @param nodesList
 * @param onDismiss
 * @param viewModel [RemoveNodeLinkViewModel]
 */
@Composable
fun RemoveNodeLinkDialog(
    modifier: Modifier = Modifier,
    nodesList: List<Long>,
    onDismiss: () -> Unit,
    viewModel: RemoveNodeLinkViewModel,
) {
    RemoveNodeLinkDialogBody(
        modifier = modifier,
        count = nodesList.size,
        onConfirmClicked = {
            viewModel.disableExport(nodesList)
            onDismiss()
        },
        onCancelClicked = {
            onDismiss()
        }
    )
}

@Composable
private fun RemoveNodeLinkDialogBody(
    modifier: Modifier = Modifier,
    count: Int,
    onConfirmClicked: () -> Unit,
    onCancelClicked: () -> Unit,
) {
    MegaAppTheme(isDark = isSystemInDarkTheme()) {
        MegaAlertDialog(
            modifier = modifier,
            text = pluralStringResource(
                id = R.plurals.remove_links_warning_text,
                count = count
            ),
            confirmButtonText = stringResource(id = R.string.general_remove),
            cancelButtonText = stringResource(id = R.string.general_cancel),
            onConfirm = onConfirmClicked,
            onDismiss = onCancelClicked,
        )
    }
}

@CombinedThemePreviews
@Composable
private fun RemoveNodeLinkDialogBodyPreview() {
    RemoveNodeLinkDialogBody(
        count = 2,
        onCancelClicked = {},
        onConfirmClicked = {},
    )
}