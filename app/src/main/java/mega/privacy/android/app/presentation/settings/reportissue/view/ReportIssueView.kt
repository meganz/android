package mega.privacy.android.app.presentation.settings.reportissue.view

import android.annotation.SuppressLint
import android.content.res.Configuration.UI_MODE_NIGHT_YES
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import mega.privacy.android.app.R
import mega.privacy.android.app.presentation.settings.reportissue.model.ReportIssueUiState
import mega.privacy.android.shared.original.core.ui.controls.dialogs.ProgressDialog
import mega.privacy.android.shared.original.core.ui.theme.OriginalTempTheme

/**
 * Report issue view from settings screen
 */
@Composable
fun ReportIssueView(
    uiState: ReportIssueUiState,
    modifier: Modifier = Modifier,
    onDescriptionChanged: (String) -> Unit = {},
    onIncludeLogsChanged: (Boolean) -> Unit = {},
    cancelUpload: () -> Unit = {},
) {

    ProgressHandler(
        cancelUpload = cancelUpload,
        uploadProgress = uiState.uploadProgress
    )

    ReportIssueContent(
        modifier = modifier,
        uiState = uiState,
        onDescriptionChanged = onDescriptionChanged,
        onIncludeLogsChanged = onIncludeLogsChanged
    )
}

@Composable
private fun ProgressHandler(
    cancelUpload: () -> Unit,
    uploadProgress: Float?,
) {
    if (uploadProgress != null) {
        ProgressDialog(
            title = stringResource(id = R.string.settings_help_report_issue_uploading_log_file),
            progress = uploadProgress,
            onCancel = cancelUpload,
            cancelButtonText = stringResource(id = R.string.general_cancel)
        )
    }
}

/**
 * Report issue view from settings screen previews
 */
@SuppressLint("UnusedMaterialScaffoldPaddingParameter")
@Preview
@Preview(
    uiMode = UI_MODE_NIGHT_YES
)
@Composable
fun PreviewReportIssueView() {
    var checkedState by remember { mutableStateOf(false) }
    OriginalTempTheme(isDark = isSystemInDarkTheme()) {
        Scaffold {
            ReportIssueView(
                uiState = ReportIssueUiState(
                    description = "",
                    includeLogs = checkedState,
                    canSubmit = true,
                    error = R.string.settings_help_report_issue_error,
                ),
                onIncludeLogsChanged = { checkedState = !checkedState },
            )
        }
    }
}
