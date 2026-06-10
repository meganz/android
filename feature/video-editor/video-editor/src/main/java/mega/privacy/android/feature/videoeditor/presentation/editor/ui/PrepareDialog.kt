package mega.privacy.android.feature.videoeditor.presentation.editor.ui

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Download
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import mega.android.core.ui.components.MegaText
import mega.android.core.ui.components.image.MegaIcon
import mega.android.core.ui.theme.AndroidThemeForPreviews
import mega.android.core.ui.theme.AppTheme
import mega.android.core.ui.theme.values.IconColor
import mega.android.core.ui.theme.values.TextColor
import mega.privacy.android.core.formatter.formatFileSize

/**
 * Blocking modal shown while the source video is fetched from MEGA into the cache, before the editor
 * can open. [onCancel] cancels the download and closes the editor.
 *
 * @param fileName name of the video being downloaded, shown as supporting text.
 * @param fileSizeBytes total size of the video in bytes, used for the size readout.
 * @param percent download progress, 0..100.
 * @param onCancel cancels the download and closes the editor.
 */
@Composable
fun PrepareVideoDialog(
    fileName: String,
    fileSizeBytes: Long,
    percent: Int,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    BlockingProgressDialog(
        title = "Preparing video",
        percent = percent,
        onCancel = onCancel,
        modifier = modifier,
        description = fileName,
    ) {
        MegaIcon(
            imageVector = Icons.Filled.Download,
            tint = IconColor.Secondary,
            contentDescription = null,
            modifier = Modifier.size(16.dp),
        )
        if (fileSizeBytes > 0L) {
            val downloadedBytes = fileSizeBytes * percent.coerceIn(0, 100) / 100
            Spacer(modifier = Modifier.width(6.dp))
            MegaText(
                text = "${formatFileSize(downloadedBytes, context)} / " +
                        formatFileSize(fileSizeBytes, context),
                style = AppTheme.typography.bodySmall,
                textColor = TextColor.Secondary,
            )
        }
    }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
private fun PrepareVideoDialogPreview() {
    AndroidThemeForPreviews {
        PrepareVideoDialog(
            fileName = "Holiday in Queenstown with entire family.mp4",
            fileSizeBytes = 52_428_800L,
            percent = 42,
            onCancel = {},
        )
    }
}
