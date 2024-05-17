package mega.privacy.android.app.presentation.login.reportissue.view

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Scaffold
import androidx.compose.material.rememberScaffoldState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import mega.privacy.android.app.R
import mega.privacy.android.app.presentation.login.reportissue.model.ReportIssueViaEmailUiState
import mega.privacy.android.app.presentation.node.model.menuaction.SendMenuAction
import mega.privacy.android.app.presentation.settings.reportissue.ReportIssueBackHandler
import mega.privacy.android.app.presentation.settings.reportissue.view.DescriptionTextField
import mega.privacy.android.app.presentation.settings.reportissue.view.ErrorBanner
import mega.privacy.android.shared.original.core.ui.controls.appbar.AppBarType
import mega.privacy.android.shared.original.core.ui.controls.appbar.MegaAppBar
import mega.privacy.android.shared.original.core.ui.controls.dividers.DividerType
import mega.privacy.android.shared.original.core.ui.controls.dividers.MegaDivider
import mega.privacy.android.shared.original.core.ui.controls.text.MegaText
import mega.privacy.android.shared.original.core.ui.preview.CombinedThemePreviews
import mega.privacy.android.shared.original.core.ui.theme.tokens.TextColor
import mega.privacy.android.shared.theme.MegaAppTheme

/**
 * Report issue via email view
 *
 * @param uiState
 * @param onDescriptionChanged
 * @param onBackPress
 * @param onDiscard
 * @param onSubmit
 */
@Composable
fun ReportIssueViaEmailView(
    uiState: ReportIssueViaEmailUiState,
    onDescriptionChanged: (String) -> Unit,
    onBackPress: () -> Unit,
    onDiscard: () -> Unit,
    onSubmit: () -> Unit,
) {

    val scaffoldState = rememberScaffoldState()

    Scaffold(
        scaffoldState = scaffoldState,
        topBar = {
            MegaAppBar(
                appBarType = AppBarType.BACK_NAVIGATION,
                title = stringResource(R.string.settings_help_report_issue),
                elevation = 0.dp,
                onNavigationPressed = onBackPress,
                actions = listOf(
                    SendMenuAction()
                ),
                onActionPressed = {
                    onSubmit()
                },
            )
        },
        content = { paddingValues ->
            ReportIssueViaEmailBody(
                modifier = Modifier.padding(paddingValues),
                uiState = uiState,
                onDescriptionChanged = onDescriptionChanged,
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


/**
 * Report issue body
 * @param modifier
 * @param uiState
 * @param onDescriptionChanged
 */
@Composable
fun ReportIssueViaEmailBody(
    modifier: Modifier,
    uiState: ReportIssueViaEmailUiState,
    onDescriptionChanged: (String) -> Unit,
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp, Alignment.Top),
        modifier = modifier.padding(horizontal = 16.dp, vertical = 16.dp),
    ) {
        if (uiState.error != null) {
            ErrorBanner(
                errorMessage = stringResource(id = uiState.error),
                modifier = Modifier.testTag(ERROR_TEST_TAG)
            )
        }

        MegaText(
            text = stringResource(R.string.settings_help_report_issue_instructions),
            textColor = TextColor.Primary,
            modifier = Modifier.testTag(NOTE_TEST_TAG)
        )

        Spacer(modifier = Modifier.height(8.dp))

        MegaDivider(dividerType = DividerType.FullSize)

        DescriptionTextField(
            description = uiState.description,
            onDescriptionChanged = onDescriptionChanged,
            modifier = Modifier
                .testTag(DESCRIPTION_FIELD_TEST_TAG)
                .fillMaxWidth()
                .defaultMinSize(minHeight = 150.dp)
        )
    }
}

internal const val ERROR_TEST_TAG = "report_issue_via_email:error"
internal const val NOTE_TEST_TAG = "report_issue_via_email:note"
internal const val DESCRIPTION_FIELD_TEST_TAG = "report_issue_via_email:description_field"


@CombinedThemePreviews
@Composable
private fun ConfirmEmailScreenPreview() {
    MegaAppTheme(isDark = isSystemInDarkTheme()) {
        ReportIssueViaEmailView(
            uiState = ReportIssueViaEmailUiState(),
            onDescriptionChanged = {},
            onBackPress = {},
            onDiscard = {},
            onSubmit = {},
        )
    }
}

@CombinedThemePreviews
@Composable
private fun ConfirmEmailScreenWithErrorPreview() {
    MegaAppTheme(isDark = isSystemInDarkTheme()) {
        ReportIssueViaEmailView(
            uiState = ReportIssueViaEmailUiState(
                error = R.string.settings_help_report_issue_description_label
            ),
            onDescriptionChanged = {},
            onBackPress = {},
            onDiscard = {},
            onSubmit = {},
        )
    }
}
