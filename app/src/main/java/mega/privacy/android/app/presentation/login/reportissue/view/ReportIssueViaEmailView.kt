package mega.privacy.android.app.presentation.login.reportissue.view

import mega.privacy.android.shared.resources.R as sharedR
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.material.Scaffold
import androidx.compose.material.rememberScaffoldState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import mega.privacy.android.app.R
import mega.privacy.android.app.presentation.login.reportissue.model.ReportIssueViaEmailUiState
import mega.privacy.android.app.presentation.node.model.menuaction.SendMenuAction
import mega.privacy.android.app.presentation.settings.reportissue.ReportIssueBackHandler
import mega.privacy.android.app.presentation.settings.reportissue.view.ReportIssueContent
import mega.privacy.android.shared.original.core.ui.controls.appbar.AppBarType
import mega.privacy.android.shared.original.core.ui.controls.appbar.MegaAppBar
import mega.privacy.android.shared.original.core.ui.preview.CombinedThemePreviews
import mega.privacy.android.shared.original.core.ui.theme.OriginalTempTheme

/**
 * Report issue via email view
 *
 * @param uiState
 * @param onDescriptionChanged
 * @param onIncludeLogsChanged
 * @param onBackPress
 * @param onDiscard
 * @param onSubmit
 */
@Composable
fun ReportIssueViaEmailView(
    uiState: ReportIssueViaEmailUiState,
    onDescriptionChanged: (String) -> Unit,
    onIncludeLogsChanged: (Boolean) -> Unit,
    onBackPress: () -> Unit,
    onDiscard: () -> Unit,
    onSubmit: () -> Unit,
) {

    val scaffoldState = rememberScaffoldState()

    Scaffold(
        modifier = Modifier.systemBarsPadding(),
        scaffoldState = scaffoldState,
        topBar = {
            MegaAppBar(
                appBarType = AppBarType.BACK_NAVIGATION,
                title = stringResource(R.string.settings_help_report_issue),
                elevation = 0.dp,
                onNavigationPressed = onBackPress,
                actions = listOf(
                    SendMenuAction(enabled = uiState.description.isNotBlank())
                ),
                onActionPressed = {
                    onSubmit()
                },
            )
        },
        content = { paddingValues ->
            ReportIssueContent(
                modifier = Modifier.padding(paddingValues),
                uiState = uiState,
                onDescriptionChanged = onDescriptionChanged,
                onIncludeLogsChanged = onIncludeLogsChanged
            )
        },
    )

    ReportIssueBackHandler(
        isEnabled = uiState.description.isNotBlank(),
        onDiscard = {
            onDiscard()
        }
    )
}

@CombinedThemePreviews
@Composable
private fun ConfirmEmailScreenPreview() {
    OriginalTempTheme(isDark = isSystemInDarkTheme()) {
        ReportIssueViaEmailView(
            uiState = ReportIssueViaEmailUiState(),
            onDescriptionChanged = {},
            onBackPress = {},
            onDiscard = {},
            onSubmit = {},
            onIncludeLogsChanged = {}
        )
    }
}

@CombinedThemePreviews
@Composable
private fun ConfirmEmailScreenWithErrorPreview() {
    OriginalTempTheme(isDark = isSystemInDarkTheme()) {
        ReportIssueViaEmailView(
            uiState = ReportIssueViaEmailUiState(
                error = sharedR.string.report_issue_error_minimum_characters
            ),
            onDescriptionChanged = {},
            onBackPress = {},
            onDiscard = {},
            onSubmit = {},
            onIncludeLogsChanged = {}
        )
    }
}
