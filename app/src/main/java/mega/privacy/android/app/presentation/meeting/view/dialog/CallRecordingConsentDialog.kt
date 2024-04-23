package mega.privacy.android.app.presentation.meeting.view.dialog

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Column
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.style.TextDecoration
import androidx.hilt.navigation.compose.hiltViewModel
import mega.privacy.android.app.R
import mega.privacy.android.app.meeting.activity.MeetingActivityViewModel
import mega.privacy.android.app.presentation.chat.ChatViewModel
import mega.privacy.android.core.ui.controls.dialogs.ConfirmationDialog
import mega.privacy.android.core.ui.controls.text.MegaSpannedClickableText
import mega.privacy.android.core.ui.controls.text.MegaText
import mega.privacy.android.core.ui.model.MegaSpanStyle
import mega.privacy.android.core.ui.model.MegaSpanStyleWithAnnotation
import mega.privacy.android.core.ui.model.SpanIndicator
import mega.privacy.android.core.ui.preview.CombinedThemePreviews
import mega.privacy.android.core.ui.theme.tokens.TextColor
import mega.privacy.android.shared.theme.MegaAppTheme

/**
 * Show call recording consent dialog
 */
@Composable
fun CallRecordingConsentDialog(
    viewModel: ChatViewModel = hiltViewModel(),
    meetingViewModel: MeetingActivityViewModel? = null
) {
    CallRecordingConsentDialog(
        onConfirm = {
            viewModel.setIsRecordingConsentAccepted(value = true)
            viewModel.setShowRecordingConsentDialogConsumed()
            meetingViewModel?.setShowRecordingConsentDialogConsumed()
        },
        onDismiss = {
            viewModel.setIsRecordingConsentAccepted(value = false)
            viewModel.setShowRecordingConsentDialogConsumed()
            meetingViewModel?.setShowRecordingConsentDialogConsumed()
        },
    )
}


/**
 * Show call recording consent dialog
 */
@Deprecated("Only required for CallRecordingConsentDialogFragment. Remove this once it is finally remove it.")
@Composable
fun CallRecordingConsentDialog(
    onDismiss: () -> Unit,
    viewModel: ChatViewModel = hiltViewModel(),
) {
    CallRecordingConsentDialog(
        onConfirm = {
            viewModel.setIsRecordingConsentAccepted(value = true)
            viewModel.setShowRecordingConsentDialogConsumed()
            onDismiss()
        },
        onDismiss = {
            viewModel.setIsRecordingConsentAccepted(value = false)
            viewModel.setShowRecordingConsentDialogConsumed()
            onDismiss()
        },
    )
}

/**
 * Show call recording consent dialog
 *
 * @param onConfirm     To be triggered when confirm button is pressed
 * @param onDismiss     To be triggered when dialog is hidden
 */
@Composable
private fun CallRecordingConsentDialog(
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    val context = LocalContext.current
    ConfirmationDialog(
        title = stringResource(id = R.string.meetings_call_recording_consent_dialog_title),
        text = {
            Column(modifier = Modifier, content = {
                MegaText(
                    text = stringResource(id = R.string.meetings_call_recording_consent_dialog_message),
                    textColor = TextColor.Secondary,
                    style = MaterialTheme.typography.subtitle1,
                )
                MegaSpannedClickableText(
                    value = stringResource(id = R.string.meetings_call_recording_consent_dialog_learn_more_option),
                    styles = hashMapOf(
                        SpanIndicator('A') to MegaSpanStyleWithAnnotation(
                            MegaSpanStyle(
                                SpanStyle(textDecoration = TextDecoration.Underline),
                                color = TextColor.Secondary,
                            ), "https://mega.io/privacy"
                        ),
                    ),
                    color = TextColor.Secondary,
                    onAnnotationClick = {
                        context.startActivity(
                            Intent(Intent.ACTION_VIEW, Uri.parse("https://mega.io/privacy"))
                        )
                    },
                    baseStyle = MaterialTheme.typography.subtitle1
                )
            })
        },
        confirmButtonText = stringResource(id = R.string.meetings_call_recording_consent_dialog_positive_button),
        cancelButtonText = stringResource(id = R.string.meetings_call_recording_consent_dialog_negative_button),
        onConfirm = onConfirm,
        onDismiss = onDismiss,
        modifier = Modifier.testTag(TEST_TAG_CALL_RECORDING_CONSENT_DIALOG),
        dismissOnClickOutside = false,
        dismissOnBackPress = false
    )
}

/**
 * [CallRecordingConsentDialog] preview
 */
@CombinedThemePreviews
@Composable
fun CallRecordingConsentDialogPreview() {
    MegaAppTheme(isDark = isSystemInDarkTheme()) {
        CallRecordingConsentDialog(
            onConfirm = {},
            onDismiss = {},
        )
    }
}

internal const val TEST_TAG_CALL_RECORDING_CONSENT_DIALOG = "meeting:call_recording_started:consent"