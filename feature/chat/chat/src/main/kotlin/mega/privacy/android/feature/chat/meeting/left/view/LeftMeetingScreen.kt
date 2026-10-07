package mega.privacy.android.feature.chat.meeting.left.view

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import mega.android.core.ui.components.MegaScaffold
import mega.android.core.ui.components.MegaText
import mega.android.core.ui.components.button.PrimaryFilledButton
import mega.android.core.ui.components.dialogs.BasicDialog
import mega.android.core.ui.components.divider.SubtleDivider
import mega.android.core.ui.components.image.MegaIcon
import mega.android.core.ui.components.snackbar.MegaSnackbar
import mega.android.core.ui.components.toolbar.AppBarNavigationType
import mega.android.core.ui.components.toolbar.MegaTopAppBar
import mega.android.core.ui.preview.CombinedThemePreviews
import mega.android.core.ui.theme.AndroidThemeForPreviews
import mega.android.core.ui.theme.AppTheme
import mega.android.core.ui.theme.spacing.LocalSpacing
import mega.android.core.ui.theme.values.IconColor
import mega.android.core.ui.theme.values.TextColor
import mega.privacy.android.feature.chat.meeting.left.model.LeftMeetingUiState
import mega.privacy.android.icon.pack.R as iconPackR
import mega.privacy.android.shared.resources.R as sharedR

/**
 * Screen a guest lands on after leaving a meeting, inviting them to create a MEGA account and
 * reporting why the call ended when it ended on a participant limit.
 *
 * @param uiState UI state of the screen.
 * @param onCloseClick Called when the guest dismisses the screen.
 * @param onCreateAccountClick Called when the guest chooses to create an account.
 * @param onNoticeShown Called once the call-end notice has been seen.
 * @param modifier [Modifier]
 */
@Composable
internal fun LeftMeetingScreen(
    uiState: LeftMeetingUiState,
    onCloseClick: () -> Unit,
    onCreateAccountClick: () -> Unit,
    onNoticeShown: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val spacing = LocalSpacing.current
    val snackbarHostState = remember { SnackbarHostState() }
    val tooManyParticipantsMessage =
        stringResource(sharedR.string.call_error_too_many_participants)

    MegaScaffold(
        modifier = modifier
            .fillMaxSize()
            .testTag(LEFT_MEETING_SCREEN_TAG),
        snackbarHost = { MegaSnackbar(snackbarHostState) },
        topBar = {
            MegaTopAppBar(
                modifier = Modifier.testTag(LEFT_MEETING_APP_BAR_TAG),
                title = "",
                navigationType = AppBarNavigationType.Close(onCloseClick),
            )
        },
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = spacing.x20),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            MegaIcon(
                modifier = Modifier.size(24.dp),
                painter = painterResource(iconPackR.drawable.ic_mega_medium_thin_outline),
                tint = IconColor.Primary,
            )

            Spacer(Modifier.height(spacing.x20))

            MegaText(
                modifier = Modifier.testTag(LEFT_MEETING_HEADLINE_TAG),
                text = stringResource(sharedR.string.more_than_meeting),
                style = AppTheme.typography.titleLarge,
                textColor = TextColor.Primary,
                textAlign = TextAlign.Center,
            )

            Image(
                modifier = Modifier.testTag(LEFT_MEETING_ILLUSTRATION_TAG),
                painter = painterResource(iconPackR.drawable.il_meeting),
                contentDescription = null,
            )

            LeftMeetingSection(
                title = stringResource(sharedR.string.left_meeting_join_title),
                body = stringResource(sharedR.string.left_meeting_join_content),
            )

            Spacer(Modifier.height(spacing.x20))

            SubtleDivider(modifier = Modifier.fillMaxWidth())

            LeftMeetingSection(
                title = stringResource(sharedR.string.meeting_guest_left_meeting_bonus_title),
                body = stringResource(sharedR.string.left_meeting_bonus_content),
            )

            Spacer(Modifier.height(spacing.x32))

            PrimaryFilledButton(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag(LEFT_MEETING_CREATE_ACCOUNT_TAG),
                text = stringResource(sharedR.string.create_account),
                onClick = onCreateAccountClick,
            )

            Spacer(Modifier.height(spacing.x24))
        }
    }

    if (uiState is LeftMeetingUiState.FreePlanParticipantsLimit) {
        BasicDialog(
            modifier = Modifier.testTag(LEFT_MEETING_FREE_PLAN_LIMIT_DIALOG_TAG),
            title = stringResource(sharedR.string.meetings_schedule_meeting_free_plan_100_participants_limit_dialog_title),
            description = stringResource(sharedR.string.meetings_schedule_meeting_free_plan_100_participants_limit_dialog_description),
            positiveButtonText = stringResource(sharedR.string.meetings_schedule_meeting_free_plan_100_participants_limit_dialog_button),
            onPositiveButtonClicked = onNoticeShown,
            onDismiss = onNoticeShown,
            dismissOnClickOutside = false,
            dismissOnBackPress = false,
        )
    }

    LaunchedEffect(uiState) {
        if (uiState is LeftMeetingUiState.TooManyParticipants) {
            snackbarHostState.showSnackbar(tooManyParticipantsMessage)
            onNoticeShown()
        }
    }
}

@Composable
private fun LeftMeetingSection(
    title: String,
    body: String,
) {
    val spacing = LocalSpacing.current

    Spacer(Modifier.height(spacing.x20))

    MegaText(
        text = title,
        style = AppTheme.typography.titleMedium,
        textColor = TextColor.Primary,
        textAlign = TextAlign.Center,
    )

    Spacer(Modifier.height(spacing.x20))

    MegaText(
        text = body,
        style = AppTheme.typography.bodyMedium,
        textColor = TextColor.Secondary,
        textAlign = TextAlign.Center,
    )
}

internal const val LEFT_MEETING_SCREEN_TAG = "left_meeting_screen:screen"
internal const val LEFT_MEETING_APP_BAR_TAG = "left_meeting_screen:app_bar"
internal const val LEFT_MEETING_HEADLINE_TAG = "left_meeting_screen:headline"
internal const val LEFT_MEETING_ILLUSTRATION_TAG = "left_meeting_screen:illustration"
internal const val LEFT_MEETING_CREATE_ACCOUNT_TAG = "left_meeting_screen:create_account"
internal const val LEFT_MEETING_FREE_PLAN_LIMIT_DIALOG_TAG =
    "left_meeting_screen:free_plan_limit_dialog"

@CombinedThemePreviews
@Composable
private fun LeftMeetingScreenPreview() {
    AndroidThemeForPreviews {
        LeftMeetingScreen(
            uiState = LeftMeetingUiState.None,
            onCloseClick = {},
            onCreateAccountClick = {},
            onNoticeShown = {},
        )
    }
}

@CombinedThemePreviews
@Composable
private fun LeftMeetingScreenFreePlanLimitPreview() {
    AndroidThemeForPreviews {
        LeftMeetingScreen(
            uiState = LeftMeetingUiState.FreePlanParticipantsLimit,
            onCloseClick = {},
            onCreateAccountClick = {},
            onNoticeShown = {},
        )
    }
}
