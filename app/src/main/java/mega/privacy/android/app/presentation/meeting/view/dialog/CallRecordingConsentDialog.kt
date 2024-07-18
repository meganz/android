package mega.privacy.android.app.presentation.meeting.view.dialog

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Column
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.style.TextDecoration
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import mega.privacy.android.app.R
import mega.privacy.android.app.presentation.meeting.CallRecordingViewModel
import mega.privacy.android.app.presentation.meeting.model.CallRecordingUIState
import mega.privacy.android.shared.original.core.ui.controls.dialogs.ConfirmationDialog
import mega.privacy.android.shared.original.core.ui.controls.text.MegaSpannedClickableText
import mega.privacy.android.shared.original.core.ui.controls.text.MegaText
import mega.privacy.android.shared.original.core.ui.model.MegaSpanStyle
import mega.privacy.android.shared.original.core.ui.model.MegaSpanStyleWithAnnotation
import mega.privacy.android.shared.original.core.ui.model.SpanIndicator
import mega.privacy.android.shared.original.core.ui.preview.CombinedThemePreviews
import mega.privacy.android.shared.original.core.ui.theme.values.TextColor
import mega.privacy.android.domain.entity.call.CallRecordingEvent
import mega.privacy.android.shared.original.core.ui.theme.OriginalTempTheme

/**
 * Show call recording consent dialog
 */
@Composable
fun CallRecordingConsentDialog(
    viewModel: CallRecordingViewModel = hiltViewModel(),
) {
    val uiState by viewModel.state.collectAsStateWithLifecycle()

    CallRecordingConsentDialog(
        uiState = uiState,
        onConfirm = {
            viewModel.setIsRecordingConsentAccepted(accepted = true)
        },
        onDismiss = {
            viewModel.setIsRecordingConsentAccepted(accepted = false)
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
    viewModel: CallRecordingViewModel = hiltViewModel(),
) {
    val uiState by viewModel.state.collectAsStateWithLifecycle()

    CallRecordingConsentDialog(
        uiState = uiState,
        onConfirm = {
            viewModel.setIsRecordingConsentAccepted(accepted = true)
            onDismiss()
        },
        onDismiss = {
            viewModel.setIsRecordingConsentAccepted(accepted = false)
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
    uiState: CallRecordingUIState,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) = with(uiState) {
    if (isSessionOnRecording && isRecordingConsentAccepted == null && isParticipatingInCall) {
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
}

/**
 * [CallRecordingConsentDialog] preview
 */
@CombinedThemePreviews
@Composable
fun CallRecordingConsentDialogPreview() {
    OriginalTempTheme(isDark = isSystemInDarkTheme()) {
        CallRecordingConsentDialog(
            uiState = CallRecordingUIState(
                callRecordingEvent = CallRecordingEvent(isSessionOnRecording = true),
            ),
            onConfirm = {},
            onDismiss = {},
        )
    }
}

internal const val TEST_TAG_CALL_RECORDING_CONSENT_DIALOG = "meeting:call_recording_started:consent"