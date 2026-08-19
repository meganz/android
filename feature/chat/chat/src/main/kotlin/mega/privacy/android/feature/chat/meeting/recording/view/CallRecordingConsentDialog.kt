package mega.privacy.android.feature.chat.meeting.recording.view

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import mega.android.core.ui.components.dialogs.BasicDialog
import mega.android.core.ui.components.text.SpannableText
import mega.android.core.ui.model.MegaSpanStyle
import mega.android.core.ui.model.SpanIndicator
import mega.android.core.ui.model.SpanStyleWithAnnotation
import mega.android.core.ui.preview.CombinedThemePreviews
import mega.android.core.ui.theme.AndroidThemeForPreviews
import mega.android.core.ui.theme.values.LinkColor
import mega.privacy.android.shared.resources.R

@Composable
fun CallRecordingConsentDialog(
    onDisplayed: () -> Unit,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    privacyUrl: String,
    onPrivacyLinkClick: (String) -> Unit,
) {
    LaunchedEffect(Unit) {
        onDisplayed()
    }
    val description =
        getDescription(privacyUrl, onPrivacyLinkClick)
    BasicDialog(
        modifier = Modifier.testTag(TEST_TAG_CALL_RECORDING_CONSENT_DIALOG),
        title = SpannableText(stringResource(id = R.string.meetings_call_recording_consent_dialog_title)),
        description = description,
        positiveButtonText = stringResource(id = R.string.meetings_call_recording_consent_dialog_positive_button),
        onPositiveButtonClicked = onConfirm,
        negativeButtonText = stringResource(id = R.string.meetings_call_recording_consent_dialog_negative_button),
        onNegativeButtonClicked = onDismiss
    )
}

@Composable
private fun getDescription(
    privacyUrl: String,
    onPrivacyLinkClick: (String) -> Unit,
): SpannableText {
    val message =
        stringResource(id = R.string.meetings_call_recording_consent_dialog_message)
    val linkText = stringResource(R.string.meetings_call_recording_consent_dialog_learn_more_option)
    val body = buildString {
        append(message)
        append("\n")
        append(linkText)
    }
    return SpannableText(
        text = body,
        annotations = hashMapOf(
            SpanIndicator('A') to SpanStyleWithAnnotation(
                MegaSpanStyle.LinkColorStyle(
                    SpanStyle(),
                    LinkColor.Primary
                ), privacyUrl
            )
        ),
        onAnnotationClick = onPrivacyLinkClick
    )
}

@CombinedThemePreviews
@Composable
private fun CallRecordingConsentDialogPreview() {
    AndroidThemeForPreviews {
        CallRecordingConsentDialog(
            onConfirm = {},
            onDismiss = {},
            privacyUrl = "",
            onPrivacyLinkClick = {},
            onDisplayed = {},
        )
    }
}


internal const val TEST_TAG_CALL_RECORDING_CONSENT_DIALOG = "meeting:call_recording_started:consent"