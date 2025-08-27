package mega.privacy.android.app.presentation.node.dialogs.removelink

import mega.privacy.android.shared.resources.R as sharedR
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import mega.privacy.android.app.R
import mega.privacy.android.core.nodecomponents.dialog.removelink.RemoveNodeLinkViewModel
import mega.privacy.android.shared.original.core.ui.controls.dialogs.MegaAlertDialog
import mega.privacy.android.shared.original.core.ui.preview.CombinedThemePreviews
import mega.privacy.android.shared.original.core.ui.theme.OriginalTheme

/**
 * Dialog to remove node link
 * @param modifier [Modifier]
 * @param nodesList
 * @param onDismiss
 * @param viewModel [mega.privacy.android.core.nodecomponents.dialog.removelink.RemoveNodeLinkViewModel]
 */
@Composable
fun RemoveNodeLinkDialog(
    nodesList: List<Long>,
    viewModel: RemoveNodeLinkViewModel = hiltViewModel(),
    onDismiss: () -> Unit,
) {
    RemoveNodeLinkDialogBody(
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
    count: Int,
    onConfirmClicked: () -> Unit,
    onCancelClicked: () -> Unit,
) {
    OriginalTheme(isDark = isSystemInDarkTheme()) {
        MegaAlertDialog(
            text = pluralStringResource(
                id = R.plurals.remove_links_warning_text,
                count = count
            ),
            confirmButtonText = stringResource(id = R.string.general_remove),
            cancelButtonText = stringResource(id = sharedR.string.general_dialog_cancel_button),
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