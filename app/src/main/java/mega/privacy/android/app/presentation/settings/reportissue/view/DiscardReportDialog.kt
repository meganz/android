package mega.privacy.android.app.presentation.settings.reportissue.view

import android.content.res.Configuration
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material.AlertDialog
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.window.DialogProperties
import mega.privacy.android.app.R
import mega.privacy.android.presentation.controls.MegaDialog
import mega.privacy.android.presentation.theme.AndroidTheme

@Composable
fun DiscardReportDialog(
    onDiscardCancelled: () -> Unit,
    onDiscard: () -> Unit,
) {
    MegaDialog(
        onDismissRequest = onDiscardCancelled,
        titleStringID = R.string.settings_help_report_issue_discard_dialog_title,
        confirmButton = {
            TextButton(
                onClick = onDiscard,
                modifier = Modifier
            ) {
                Text(
                    text = stringResource(id = R.string.settings_help_report_issue_discard_button),
                    style = MaterialTheme.typography.button,
                    color = if (!MaterialTheme.colors.isLight) colorResource(id = R.color.teal_200) else colorResource(
                        id = R.color.teal_300
                    )
                )
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDiscardCancelled,
                modifier = Modifier
            ) {
                Text(
                    text = stringResource(id = R.string.general_cancel),
                    style = MaterialTheme.typography.button,
                    color = if (!MaterialTheme.colors.isLight) colorResource(id = R.color.teal_200) else colorResource(
                        id = R.color.teal_300
                    )
                )
            }
        }
    )
}

@Preview
@Preview(
    uiMode = Configuration.UI_MODE_NIGHT_YES
)
@Composable
fun PreviewDiscardReportDialog() {
    AndroidTheme(isDark = isSystemInDarkTheme()) {
        DiscardReportDialog(onDiscardCancelled = {},
            onDiscard = {})
    }
}