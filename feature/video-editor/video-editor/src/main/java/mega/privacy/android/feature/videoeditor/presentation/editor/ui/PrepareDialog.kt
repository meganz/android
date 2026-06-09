package mega.privacy.android.feature.videoeditor.presentation.editor.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Download
import androidx.compose.material3.BasicAlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import mega.android.core.ui.components.MegaText
import mega.android.core.ui.components.button.TextOnlyButtonM3
import mega.android.core.ui.components.image.MegaIcon
import mega.android.core.ui.components.indicators.MegaAnimatedLinearProgressIndicator
import mega.android.core.ui.components.surface.ColumnSurface
import mega.android.core.ui.components.surface.SurfaceColor
import mega.android.core.ui.theme.AppTheme
import mega.android.core.ui.theme.values.IconColor
import mega.android.core.ui.theme.values.TextColor
import mega.privacy.android.core.formatter.formatFileSize

/**
 * Blocking modal shown while the source video is fetched from MEGA into the cache, before the editor
 * can open. A centered card with the file name, a determinate progress bar annotated with the
 * downloaded / total size and percentage, and a Cancel action.
 *
 * The dialog cannot be dismissed by tapping outside; [onCancel] (the Cancel button or a back press)
 * cancels the download and closes the editor.
 *
 * @param fileName name of the video being downloaded, shown as supporting text.
 * @param fileSizeBytes total size of the video in bytes, used for the size readout.
 * @param percent download progress, 0..100.
 * @param onCancel cancels the download and closes the editor.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PrepareVideoDialog(
    fileName: String,
    fileSizeBytes: Long,
    percent: Int,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    BasicAlertDialog(
        onDismissRequest = onCancel,
        modifier = modifier,
        properties = DialogProperties(dismissOnClickOutside = false),
    ) {
        ColumnSurface(
            surfaceColor = SurfaceColor.Surface1,
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(28.dp)),
        ) {
            Column(modifier = Modifier.padding(24.dp)) {
                MegaText(
                    text = "Preparing video",
                    style = AppTheme.typography.headlineSmall,
                    textColor = TextColor.Primary,
                )
                Spacer(modifier = Modifier.height(8.dp))
                MegaText(
                    text = fileName,
                    style = AppTheme.typography.bodyMedium,
                    textColor = TextColor.Secondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(modifier = Modifier.height(24.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
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
                    MegaText(
                        text = "$percent%",
                        style = AppTheme.typography.titleSmall,
                        textColor = TextColor.Brand,
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                MegaAnimatedLinearProgressIndicator(
                    indicatorProgress = percent / 100f,
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(modifier = Modifier.height(24.dp))
                TextOnlyButtonM3(
                    text = "Cancel",
                    onClick = onCancel,
                    modifier = Modifier.align(Alignment.End),
                )
            }
        }
    }
}
