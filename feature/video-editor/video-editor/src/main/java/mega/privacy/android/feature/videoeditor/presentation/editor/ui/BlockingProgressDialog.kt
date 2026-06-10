package mega.privacy.android.feature.videoeditor.presentation.editor.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import mega.android.core.ui.components.MegaText
import mega.android.core.ui.components.button.TextOnlyButtonM3
import mega.android.core.ui.components.indicators.MegaAnimatedLinearProgressIndicator
import mega.android.core.ui.components.surface.ColumnSurface
import mega.android.core.ui.components.surface.SurfaceColor
import mega.android.core.ui.theme.AppTheme
import mega.android.core.ui.theme.values.TextColor

/**
 * Shared shell for the editor's blocking progress dialogs ([PrepareVideoDialog],
 * [ExportProgressDialog]): a centered card with a title, an optional ellipsized
 * supporting line, a status row annotated with the percentage, a determinate
 * progress bar, and a Cancel action.
 *
 * The dialog cannot be dismissed by tapping outside — these dialogs guard
 * long-running work an accidental scrim tap must not abort. [onCancel] (the
 * Cancel button or a back press) is the only way out.
 *
 * @param title headline of the dialog.
 * @param percent progress, 0..100.
 * @param onCancel cancels the guarded work.
 * @param description optional ellipsized line under the title (e.g. a file name).
 * @param statusContent left side of the status row, opposite the percentage readout.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun BlockingProgressDialog(
    title: String,
    percent: Int,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier,
    description: String? = null,
    statusContent: @Composable RowScope.() -> Unit,
) {
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
                    text = title,
                    style = AppTheme.typography.headlineSmall,
                    textColor = TextColor.Primary,
                )
                if (!description.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    MegaText(
                        text = description,
                        style = AppTheme.typography.bodyMedium,
                        textColor = TextColor.Secondary,
                        maxLines = 1,
                        overflow = TextOverflow.MiddleEllipsis,
                    )
                }
                Spacer(modifier = Modifier.height(24.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        content = statusContent,
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
