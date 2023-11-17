package mega.privacy.android.app.presentation.meeting.view

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Column
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.style.TextDecoration
import mega.privacy.android.app.R
import mega.privacy.android.core.ui.controls.dialogs.ConfirmationDialog
import mega.privacy.android.core.ui.controls.text.MegaSpannedClickableText
import mega.privacy.android.core.ui.controls.text.MegaText
import mega.privacy.android.core.ui.model.MegaSpanStyle
import mega.privacy.android.core.ui.model.MegaSpanStyleWithAnnotation
import mega.privacy.android.core.ui.model.SpanIndicator
import mega.privacy.android.core.ui.preview.CombinedThemePreviews
import mega.privacy.android.core.ui.theme.AndroidTheme
import mega.privacy.android.core.ui.theme.tokens.TextColor

/**
 * Show call recording consent dialog
 *
 * @param onConfirm     To be triggered when confirm button is pressed
 * @param onDismiss     To be triggered when dialog is hidden
 * @param onLearnMore   To be triggered when "Learn more" option is pressed
 */
@Composable
fun CallRecordingConsentDialog(
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    onLearnMore: () -> Unit,
) {
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
                        onLearnMore()
                    },
                    baseStyle = MaterialTheme.typography.subtitle1
                )
            })
        },
        confirmButtonText = stringResource(id = R.string.meetings_call_recording_consent_dialog_positive_button),
        cancelButtonText = stringResource(id = R.string.meetings_call_recording_consent_dialog_negative_button),
        onConfirm = onConfirm,
        onDismiss = onDismiss,
        modifier = Modifier.testTag(TEST_TAG_CALL_RECORDING_CONSENT_DIALOG)
    )
}

/**
 * [CallRecordingConsentDialog] preview
 */
@CombinedThemePreviews
@Composable
fun CallRecordingConsentDialogPreview() {
    AndroidTheme(isDark = isSystemInDarkTheme()) {
        CallRecordingConsentDialog(
            onConfirm = {},
            onDismiss = {},
            onLearnMore = {},
        )
    }
}

internal const val TEST_TAG_CALL_RECORDING_CONSENT_DIALOG = "meeting:call_recording_started:consent"