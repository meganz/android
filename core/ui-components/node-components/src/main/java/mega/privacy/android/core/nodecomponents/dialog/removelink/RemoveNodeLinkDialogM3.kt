package mega.privacy.android.core.nodecomponents.dialog.removelink

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import androidx.hilt.navigation.compose.hiltViewModel
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
    BasicDialog(
        description = pluralStringResource(
            id = sharedR.plurals.remove_links_warning_text,
            count = nodes.size
        ),
        positiveButtonText = stringResource(id = sharedR.string.general_remove),
        negativeButtonText = stringResource(id = sharedR.string.general_dialog_cancel_button),
        onPositiveButtonClicked = {
            viewModel.disableExport(nodes)
            onDismiss()
        },
        onNegativeButtonClicked = onDismiss,
    )
}

@CombinedThemePreviews
@Composable
private fun RemoveNodeLinkDialogM3PreviewPlurals(
    @PreviewParameter(CountProvider::class) nodes: List<Long>,
) {
    AndroidThemeForPreviews {
        RemoveNodeLinkDialogM3(
            nodes = nodes,
            onDismiss = {},
        )
    }
}


private class CountProvider : PreviewParameterProvider<List<Long>> {
    override val values = listOf(
        listOf(1L),
        listOf(1L, 2L)
    ).asSequence()
}