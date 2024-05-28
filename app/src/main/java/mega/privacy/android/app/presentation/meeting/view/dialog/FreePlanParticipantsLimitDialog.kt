package mega.privacy.android.app.presentation.meeting.view.dialog

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import mega.privacy.android.app.R
import mega.privacy.android.shared.original.core.ui.theme.OriginalTempTheme
import mega.privacy.android.shared.original.core.ui.controls.dialogs.ConfirmationDialog
import mega.privacy.android.shared.original.core.ui.preview.CombinedThemePreviews

/**
 * Show free plan participants limit dialog
 *
 * @param onConfirm     To be triggered when confirm button is pressed
 */
@Composable
fun FreePlanLimitParticipantsDialog(
    onConfirm: () -> Unit,
) {
    ConfirmationDialog(
        title = stringResource(id = R.string.meetings_schedule_meeting_free_plan_100_participants_limit_dialog_title),
        text = stringResource(id = R.string.meetings_schedule_meeting_free_plan_100_participants_limit_dialog_description),
        confirmButtonText = stringResource(id = R.string.meetings_schedule_meeting_free_plan_100_participants_limit_dialog_button),
        cancelButtonText = null,
        onConfirm = onConfirm,
        onDismiss = onConfirm,
        modifier = Modifier.testTag(TEST_TAG_FREE_PLAN_LIMIT_PARTICIPANTS_DIALOG),
        dismissOnClickOutside = false,
        dismissOnBackPress = false
    )
}

/**
 * [FreePlanLimitParticipantsDialog] preview
 */
@CombinedThemePreviews
@Composable
fun FreePlanLimitParticipantsDialogPreview() {
    OriginalTempTheme(isDark = isSystemInDarkTheme()) {
        FreePlanLimitParticipantsDialog(
            onConfirm = {},
        )
    }
}

internal const val TEST_TAG_FREE_PLAN_LIMIT_PARTICIPANTS_DIALOG =
    "meeting:free_plan_limit_participants:consent"