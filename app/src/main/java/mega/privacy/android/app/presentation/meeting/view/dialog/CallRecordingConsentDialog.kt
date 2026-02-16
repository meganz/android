package mega.privacy.android.app.presentation.meeting.view.dialog

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Column
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.style.TextDecoration
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import mega.android.core.ui.theme.values.TextColor
import mega.privacy.android.feature.chat.meeting.recording.CallRecordingViewModel
import mega.privacy.android.feature.chat.meeting.recording.model.CallRecordingUIState
import mega.privacy.android.shared.original.core.ui.controls.dialogs.ConfirmationDialog
import mega.privacy.android.shared.original.core.ui.controls.text.MegaSpannedClickableText
import mega.privacy.android.shared.original.core.ui.controls.text.MegaText
import mega.privacy.android.shared.original.core.ui.model.MegaSpanStyle
import mega.privacy.android.shared.original.core.ui.model.MegaSpanStyleWithAnnotation
import mega.privacy.android.shared.original.core.ui.model.SpanIndicator
import mega.privacy.android.shared.original.core.ui.preview.CombinedThemePreviews
import mega.privacy.android.shared.original.core.ui.theme.OriginalTheme
import mega.privacy.android.shared.resources.R

private const val privacyUrl = "https://mega.io/privacy"

/**
 * Show call recording consent dialog
 */
@Composable
fun CallRecordingConsentDialog(
    viewModel: CallRecordingViewModel = hiltViewModel(),
    openWebView: (String) -> Unit,
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
        onPrivacyLinkClick = {
            openWebView(privacyUrl)
        }
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
    onPrivacyLinkClick: () -> Unit,
) = with(uiState) {
    if (requiresRecordingConsent) {
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
                                ), privacyUrl
                            ),
                        ),
                        color = TextColor.Secondary,
                        onAnnotationClick = { onPrivacyLinkClick() },
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
    OriginalTheme(isDark = isSystemInDarkTheme()) {
        CallRecordingConsentDialog(
            uiState = CallRecordingUIState(
                isSessionOnRecording = true,
            ),
            onConfirm = {},
            onDismiss = {},
            onPrivacyLinkClick = {},
        )
    }
}

internal const val TEST_TAG_CALL_RECORDING_CONSENT_DIALOG = "meeting:call_recording_started:consent"