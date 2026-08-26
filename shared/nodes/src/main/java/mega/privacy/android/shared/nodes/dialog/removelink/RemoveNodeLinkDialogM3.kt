package mega.privacy.android.shared.nodes.dialog.removelink

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import mega.android.core.ui.components.dialogs.BasicDialog
import mega.android.core.ui.preview.CombinedThemePreviews
import mega.android.core.ui.theme.AndroidThemeForPreviews
import mega.privacy.android.shared.resources.R as sharedR

@Composable
fun RemoveNodeLinkDialogM3(
    nodes: List<Long>,
    onDismiss: () -> Unit,
    viewModel: RemoveNodeLinkViewModel = hiltViewModel()
) {
    RemoveNodeLinkDialogBodyM3(
        count = nodes.size,
        onConfirm = {
            viewModel.disableExport(nodes)
            onDismiss()
        },
        onDismiss = onDismiss,
    )
}

@Composable
internal fun RemoveNodeLinkDialogBodyM3(
    count: Int,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    BasicDialog(
        modifier = Modifier.testTag(REMOVE_NODE_LINK_DIALOG_TAG),
        title = pluralStringResource(sharedR.plurals.remove_link_dialog_title, count),
        description = pluralStringResource(sharedR.plurals.remove_link_dialog_description, count),
        positiveButtonText = stringResource(id = sharedR.string.general_remove),
        negativeButtonText = stringResource(id = sharedR.string.general_dialog_cancel_button),
        onPositiveButtonClicked = onConfirm,
        onNegativeButtonClicked = onDismiss,
    )
}

@CombinedThemePreviews
@Composable
private fun RemoveNodeLinkDialogM3PreviewPlurals(
    @PreviewParameter(CountProvider::class) count: Int,
) {
    AndroidThemeForPreviews {
        RemoveNodeLinkDialogBodyM3(
            count = count,
            onConfirm = {},
            onDismiss = {},
        )
    }
}

private class CountProvider : PreviewParameterProvider<Int> {
    override val values = sequenceOf(1, 2)
}

internal const val REMOVE_NODE_LINK_DIALOG_TAG = "remove_node_link:dialog"
