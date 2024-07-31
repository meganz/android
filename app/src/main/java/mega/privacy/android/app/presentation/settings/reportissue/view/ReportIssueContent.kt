package mega.privacy.android.app.presentation.settings.reportissue.view

import mega.privacy.android.shared.resources.R as sharedR
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import mega.privacy.android.app.R
import mega.privacy.android.app.presentation.settings.reportissue.model.ReportIssueContentUiState
import mega.privacy.android.legacy.core.ui.controls.controlssliders.LabelledSwitch
import mega.privacy.android.shared.original.core.ui.controls.dividers.DividerType
import mega.privacy.android.shared.original.core.ui.controls.dividers.MegaDivider
import mega.privacy.android.shared.original.core.ui.controls.text.MegaText
import mega.privacy.android.shared.original.core.ui.controls.textfields.GenericTextField
import mega.privacy.android.shared.original.core.ui.theme.values.TextColor

/**
 * Report issue content body
 */
@Composable
fun ReportIssueContent(
    modifier: Modifier,
    uiState: ReportIssueContentUiState,
    onDescriptionChanged: (String) -> Unit,
    onIncludeLogsChanged: (Boolean) -> Unit,
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp, Alignment.Top),
        modifier = modifier.padding(vertical = 16.dp),
    ) {
        MegaText(
            modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 16.dp),
            text = stringResource(R.string.settings_help_report_issue_instructions),
            textColor = TextColor.Primary,
        )

        MegaDivider(dividerType = DividerType.Centered)

        GenericTextField(
            modifier = Modifier.padding(horizontal = 16.dp),
            textFieldModifier = Modifier
                .defaultMinSize(minHeight = 100.dp),
            text = uiState.description,
            singleLine = false,
            onTextChange = onDescriptionChanged,
            errorText = uiState.error?.let { stringResource(it) },
            placeholder = stringResource(sharedR.string.report_issue_description_placeholder_message)
        )

        if (uiState.includeLogsVisible) {
            LabelledSwitch(
                label = stringResource(id = R.string.settings_help_report_issue_attach_logs_label),
                checked = uiState.includeLogs,
                onCheckChanged = onIncludeLogsChanged,
                modifier = Modifier
                    .padding(
                        top = if (uiState.error != null) 16.dp else 0.dp,
                        start = 16.dp,
                        end = 16.dp
                    )
                    .fillMaxWidth(),
            )

            MegaDivider(dividerType = DividerType.FullSize)
        }
    }
}
