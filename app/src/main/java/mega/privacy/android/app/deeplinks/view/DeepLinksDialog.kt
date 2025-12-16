package mega.privacy.android.app.deeplinks.view

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.navigation3.runtime.NavKey
import mega.android.core.ui.components.dialogs.BasicSpinnerDialog
import mega.android.core.ui.theme.AndroidThemeForPreviews
import mega.privacy.android.app.R
import mega.privacy.android.app.deeplinks.model.DeepLinksUIState
import mega.privacy.android.shared.original.core.ui.preview.CombinedThemeRtlPreviews

@Composable
internal fun DeepLinksDialog(
    uiState: DeepLinksUIState,
    onNavigate: (List<NavKey>) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    with(uiState) {
        if (navKeys == null) {
            BasicSpinnerDialog(
                modifier = modifier.testTag(DEEP_LINK_DIALOG_TEST_TAG),
                contentText = stringResource(R.string.processing_link),
                onDismiss = onDismiss
            )
        } else {
            onDismiss()

            if (navKeys.isNotEmpty()) {
                onNavigate(navKeys)
            }
        }
    }
}

@CombinedThemeRtlPreviews
@Composable
private fun DeepLinksDialogPreview() {
    AndroidThemeForPreviews {
        Box(modifier = Modifier.fillMaxSize()) {
            DeepLinksDialog(
                uiState = DeepLinksUIState(),
                onNavigate = {},
                onDismiss = {},
            )
        }
    }
}

internal const val DEEP_LINK_DIALOG_TEST_TAG = "deep_link_dialog"