package mega.privacy.android.feature.videoeditor.presentation.editor.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.BasicAlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import mega.android.core.ui.components.MegaText
import mega.android.core.ui.components.button.TextOnlyButtonM3
import mega.android.core.ui.components.indicators.MegaAnimatedLinearProgressIndicator
import mega.android.core.ui.components.surface.ColumnSurface
import mega.android.core.ui.components.surface.SurfaceColor
import mega.android.core.ui.theme.AppTheme
import mega.android.core.ui.theme.values.TextColor

/**
 * Modal dialog shown while an export is encoding. Mirrors the download dialog:
 * a centered card with a determinate progress bar and a single Cancel action.
 *
 * @param percent encode progress, 0..100.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExportProgressDialog(
    percent: Int,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier,
) {
    BasicAlertDialog(onDismissRequest = onCancel, modifier = modifier) {
        ColumnSurface(
            surfaceColor = SurfaceColor.Surface1,
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(28.dp)),
        ) {
            Column(modifier = Modifier.padding(24.dp)) {
                MegaText(
                    text = "Saving copy",
                    style = AppTheme.typography.headlineSmall,
                    textColor = TextColor.Primary,
                )
                Spacer(modifier = Modifier.height(24.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    MegaText(
                        text = "Encoding…",
                        style = AppTheme.typography.bodySmall,
                        textColor = TextColor.Secondary,
                    )
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
                Spacer(modifier = Modifier.height(16.dp))
                TextOnlyButtonM3(
                    text = "Cancel",
                    onClick = onCancel,
                    modifier = Modifier.align(Alignment.End),
                )
            }
        }
    }
}
