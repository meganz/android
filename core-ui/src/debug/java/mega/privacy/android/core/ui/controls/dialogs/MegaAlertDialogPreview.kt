package mega.privacy.android.core.ui.controls.dialogs

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import mega.privacy.android.core.R
import mega.privacy.android.core.ui.preview.CombinedThemeRtlPreviews
import mega.privacy.android.core.ui.theme.AndroidTheme

@CombinedThemeRtlPreviews
@Composable
private fun MegaAlertDialogPreview() {
    AndroidTheme(isDark = isSystemInDarkTheme()) {
        PreviewBox {
            MegaAlertDialog(
                text = stringResource(id = R.string.dialog_text),
                confirmButtonText = stringResource(id = R.string.discard),
                cancelButtonText = stringResource(id = R.string.cancel),
                onConfirm = {},
                onDismiss = {},
            )
        }
    }
}

@CombinedThemeRtlPreviews
@Composable
private fun MegaAlertDialogPreviewLongAction() {
    AndroidTheme(isDark = isSystemInDarkTheme()) {
        PreviewBox {
            MegaAlertDialog(
                text = stringResource(id = R.string.dialog_text),
                confirmButtonText = stringResource(id = R.string.action_long),
                cancelButtonText = stringResource(id = R.string.cancel_long),
                onConfirm = {},
                onDismiss = {},
            )
        }
    }
}

@CombinedThemeRtlPreviews
@Composable
private fun MegaAlertDialogPreviewTitle() {
    AndroidTheme(isDark = isSystemInDarkTheme()) {
        PreviewBox {
            MegaAlertDialog(
                title = stringResource(id = R.string.dialog_title),
                text = stringResource(id = R.string.dialog_text_long),
                confirmButtonText = stringResource(id = R.string.discard),
                cancelButtonText = stringResource(id = R.string.cancel),
                onConfirm = {},
                onDismiss = {},
            )
        }
    }
}

@Composable
private fun PreviewBox(content: @Composable BoxScope.() -> Unit) = Box(
    modifier = Modifier.padding(horizontal = 240.dp, vertical = 120.dp),
    content = content
)