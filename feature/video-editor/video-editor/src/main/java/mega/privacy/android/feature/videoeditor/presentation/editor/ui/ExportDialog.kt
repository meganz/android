package mega.privacy.android.feature.videoeditor.presentation.editor.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import mega.android.core.ui.components.MegaText
import mega.android.core.ui.theme.AndroidThemeForPreviews
import mega.android.core.ui.theme.AppTheme
import mega.android.core.ui.theme.values.TextColor
import mega.privacy.android.feature.videoeditor.components.BlockingProgressDialog

/**
 * Modal dialog shown while encoding the edited copy. [onCancel] cancels the export; the user's
 * edits survive, so a restart only costs the encode time.
 *
 * @param percent encode progress, 0..100.
 * @param fileName name the copy is uploaded under (before collision renaming), shown as
 * supporting text; hidden when blank.
 * @param onCancel cancels the export.
 */
@Composable
fun ExportProgressDialog(
    percent: Int,
    fileName: String,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier,
) {
    BlockingProgressDialog(
        title = "Saving copy",
        percent = percent,
        onCancel = onCancel,
        modifier = modifier,
        description = fileName.takeIf { it.isNotBlank() },
    ) {
        MegaText(
            text = "Encoding…",
            style = AppTheme.typography.bodySmall,
            textColor = TextColor.Secondary,
        )
    }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
private fun ExportProgressDialogPreview() {
    AndroidThemeForPreviews {
        ExportProgressDialog(
            percent = 42,
            fileName = "Holiday in Queenstown.mp4",
            onCancel = {},
        )
    }
}
