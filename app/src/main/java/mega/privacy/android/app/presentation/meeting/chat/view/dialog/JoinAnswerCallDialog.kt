package mega.privacy.android.app.presentation.meeting.chat.view.dialog

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import mega.privacy.android.app.R
import mega.privacy.android.core.ui.controls.dialogs.ConfirmationDialog
import mega.privacy.android.core.ui.preview.CombinedThemePreviews
import mega.privacy.android.shared.theme.MegaAppTheme

@Composable
internal fun JoinAnswerCallDialog(
    isGroup: Boolean,
    numberOfCallsInOtherChats: Int,
    onHoldAndAnswer: () -> Unit = {},
    onEndAndAnswer: () -> Unit = {},
    onDismiss: () -> Unit = {},
) = if (numberOfCallsInOtherChats > 1) {
    ConfirmationDialog(
        title = stringResource(id = if (isGroup) R.string.title_join_call else R.string.title_join_one_to_one_call),
        text = stringResource(id = R.string.text_join_call),
        confirmButtonText = stringResource(id = if (isGroup) R.string.end_and_join_call_incoming else R.string.end_and_answer_call_incoming),
        cancelButtonText = stringResource(id = R.string.general_cancel),
        onConfirm = onEndAndAnswer,
        onDismiss = onDismiss,
        modifier = Modifier.testTag(TEST_TAG_JOIN_ANSWER_CALL_DIALOG)
    )
} else {
    ConfirmationDialog(
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
}

@CombinedThemePreviews
@Composable
private fun JoinAnswerCallDialogPreview(
    @PreviewParameter(JoinAnswerCallDialogPreviewProvider::class) status: JoinAnswerCallDialogStatus,
) {
    MegaAppTheme(isDark = isSystemInDarkTheme()) {
        with(status) {
            JoinAnswerCallDialog(
                isGroup = isGroup,
                numberOfCallsInOtherChats = numberOfCallsInOtherChats
            )
        }
    }
}

/**
 * A data class for providing preview states for the [JoinAnswerCallDialog].
 */
internal data class JoinAnswerCallDialogStatus(
    val isGroup: Boolean,
    val numberOfCallsInOtherChats: Int,
)

/**
 * A class that provides Preview Parameters for [JoinAnswerCallDialogStatus].
 */
internal class JoinAnswerCallDialogPreviewProvider :
    PreviewParameterProvider<JoinAnswerCallDialogStatus> {

    override val values: Sequence<JoinAnswerCallDialogStatus>
        get() = sequenceOf(
            JoinAnswerCallDialogStatus(isGroup = false, numberOfCallsInOtherChats = 1),
            JoinAnswerCallDialogStatus(isGroup = true, numberOfCallsInOtherChats = 1),
            JoinAnswerCallDialogStatus(isGroup = false, numberOfCallsInOtherChats = 2),
            JoinAnswerCallDialogStatus(isGroup = false, numberOfCallsInOtherChats = 2),
        )
}

internal const val TEST_TAG_JOIN_ANSWER_CALL_DIALOG = "chat_view_join_answer_call_dialog"