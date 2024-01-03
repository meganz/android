package mega.privacy.android.app.presentation.meeting.chat.view.dialog

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.PreviewParameter
import mega.privacy.android.app.R
import mega.privacy.android.core.ui.controls.dialogs.ConfirmationDialog
import mega.privacy.android.core.ui.preview.BooleanProvider
import mega.privacy.android.core.ui.preview.CombinedThemePreviews
import mega.privacy.android.shared.theme.MegaAppTheme

@Composable
internal fun JoinAnswerCallDialog(
    isGroup: Boolean,
    onHoldAndAnswer: () -> Unit = {},
    onEndAndAnswer: () -> Unit = {},
    onDismiss: () -> Unit = {},
) = ConfirmationDialog(
    title = stringResource(id = if (isGroup) R.string.title_join_call else R.string.title_join_one_to_one_call),
    text = stringResource(id = R.string.text_join_another_call),
    buttonOption1Text = stringResource(id = if (isGroup) R.string.hold_and_join_call_incoming else R.string.hold_and_answer_call_incoming),
    buttonOption2Text = stringResource(id = if (isGroup) R.string.end_and_join_call_incoming else R.string.end_and_answer_call_incoming),
    cancelButtonText = stringResource(id = R.string.general_cancel),
    onOption1 = onHoldAndAnswer,
    onOption2 = onEndAndAnswer,
    onDismiss = onDismiss,
    modifier = Modifier.testTag(TEST_TAG_JOIN_ANSWER_CALL_DIALOG)
)

@CombinedThemePreviews
@Composable
private fun JoinAnswerCallDialogPreview(
    @PreviewParameter(BooleanProvider::class) isGroup: Boolean,
) {
    MegaAppTheme(isDark = isSystemInDarkTheme()) {
        JoinAnswerCallDialog(isGroup = isGroup)
    }
}

internal const val TEST_TAG_JOIN_ANSWER_CALL_DIALOG = "chat_view_join_answer_call_dialog"