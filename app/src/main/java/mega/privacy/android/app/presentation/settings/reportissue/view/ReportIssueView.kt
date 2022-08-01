package mega.privacy.android.app.presentation.settings.reportissue.view

import android.annotation.SuppressLint
import android.content.res.Configuration.UI_MODE_NIGHT_YES
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Divider
import androidx.compose.material.Scaffold
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.layout
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import mega.privacy.android.app.R
import mega.privacy.android.app.presentation.controls.LabelledSwitch
import mega.privacy.android.app.presentation.controls.ProgressDialog
import mega.privacy.android.app.presentation.settings.reportissue.model.ReportIssueState
import mega.privacy.android.app.presentation.theme.AndroidTheme

@Composable
fun ReportIssueView(
    state: ReportIssueState,
    modifier: Modifier = Modifier,
    onDescriptionChanged: (String) -> Unit = {},
    onIncludeLogsChanged: (Boolean) -> Unit = {},
    cancelUpload: () -> Unit = {},
    onDiscard: () -> Unit = {},
    onNavigationCancelled: () -> Unit = {},
) {
    HandleDialogs(state, onNavigationCancelled, onDiscard, cancelUpload)

    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp, Alignment.Top),
        modifier = modifier.padding(all = 8.dp),
    ) {

        if (state.error != null) {
            ErrorBanner(
                errorMessage = stringResource(id = state.error),
                modifier = Modifier
                    .layout { measurable, constraints ->
                        val placeable = measurable.measure(
                            constraints.copy(
                                maxWidth = constraints.maxWidth + 16.dp.roundToPx()
                            )
                        )
                        layout(placeable.width, placeable.height) {
                            placeable.place(0, 0)
                        }
                    })

        }

        Text(text = stringResource(R.string.settings_help_report_issue_instructions))
        Divider(color = colorResource(id = R.color.grey_alpha_012), thickness = 1.dp)
        DescriptionTextField(
            description = state.description,
            onDescriptionChanged = onDescriptionChanged,
            modifier = Modifier
                .fillMaxWidth()
                .weight(1.0f)
        )
        if (state.includeLogsVisible) {
            LabelledSwitch(
                label = stringResource(id = R.string.settings_help_report_issue_attach_logs_label),
                checked = state.includeLogs,
                onCheckChanged = onIncludeLogsChanged,
                modifier = Modifier
                    .padding(vertical = 8.dp)
                    .fillMaxWidth(),
            )
        }
        Spacer(modifier = Modifier.weight(1.0f))
    }
}

@Composable
private fun HandleDialogs(
    state: ReportIssueState,
    onNavigationCancelled: () -> Unit,
    onDiscard: () -> Unit,
    cancelUpload: () -> Unit,
) {
    if (state.navigationRequested) {
        DiscardReportDialog(onNavigationCancelled, onDiscard)
    } else if (state.uploadProgress != null) ProgressDialog(
        title = stringResource(id = R.string.settings_help_report_issue_uploading_log_file),
        progress = state.uploadProgress,
        onCancel = cancelUpload,
    )
}

@SuppressLint("UnusedMaterialScaffoldPaddingParameter")
@Preview
@Preview(
    uiMode = UI_MODE_NIGHT_YES
)
@Composable
fun PreviewReportIssueView() {
    var checkedState by remember { mutableStateOf(false) }
    AndroidTheme(isDark = isSystemInDarkTheme()) {
        Scaffold {
            ReportIssueView(
                state = ReportIssueState(
                    description = "",
                    includeLogs = checkedState,
                    canSubmit = true,
                    includeLogsVisible = true,
                    error = R.string.settings_help_report_issue_error,
                ),
                onIncludeLogsChanged = { checkedState = !checkedState },
            )
        }
    }
}
