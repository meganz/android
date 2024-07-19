package mega.privacy.android.app.presentation.cancelaccountplan.view.instructionscreens

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import mega.privacy.android.app.presentation.cancelaccountplan.view.GenericInstructionStep
import mega.privacy.android.app.presentation.cancelaccountplan.view.InstructionStepWithBoldText
import mega.privacy.android.app.utils.APPLE_SUPPORT_URL
import mega.privacy.android.shared.original.core.ui.controls.text.MegaSpannedClickableText
import mega.privacy.android.shared.original.core.ui.controls.text.MegaText
import mega.privacy.android.shared.original.core.ui.model.MegaSpanStyle
import mega.privacy.android.shared.original.core.ui.model.MegaSpanStyleWithAnnotation
import mega.privacy.android.shared.original.core.ui.model.SpanIndicator
import mega.privacy.android.shared.original.core.ui.preview.CombinedThemePreviews
import mega.privacy.android.shared.original.core.ui.theme.OriginalTempTheme
import mega.privacy.android.shared.original.core.ui.theme.extensions.h6Medium
import mega.privacy.android.shared.original.core.ui.theme.extensions.subtitle1medium
import mega.privacy.android.shared.original.core.ui.theme.values.TextColor
import mega.privacy.android.shared.resources.R

/**
 * View to show the instructions to cancel the subscription on Apple devices
 */
@Composable
internal fun AppleInstructionsView(onCancelSubsFromOtherDeviceClicked: (url: String) -> Unit) {

    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .verticalScroll(scrollState)
            .padding(20.dp)
            .testTag(APPLE_INSTRUCTIONS_VIEW_TEST_TAG)
    ) {
        MegaText(
            text = stringResource(id = R.string.account_cancellation_instructions_not_managed_by_google),
            textColor = TextColor.Primary,
            style = MaterialTheme.typography.h6Medium,
            modifier = Modifier
                .padding(top = 10.dp)
                .testTag(APPLE_INSTRUCTIONS_TITLE_TEST_TAG),
        )
        MegaText(
            text = stringResource(id = R.string.account_cancellation_instructions_message_apple_description),
            textColor = TextColor.Secondary,
            style = MaterialTheme.typography.subtitle1,
            textAlign = TextAlign.Start,
            modifier = Modifier
                .padding(top = 20.dp)
                .testTag(APPLE_INSTRUCTIONS_SUBTITLE_TEST_TAG),
        )

        MegaText(
            text = stringResource(id = R.string.account_cancellation_instructions_ios_device),
            textColor = TextColor.Primary,
            style = MaterialTheme.typography.subtitle1medium,
            modifier = Modifier
                .padding(top = 30.dp, end = 8.dp)
                .testTag(APPLE_INSTRUCTIONS_HEADER_TEST_TAG),
        )

        InstructionStepWithBoldText(stepResId = R.string.account_cancellation_instructions_open_settings)
        GenericInstructionStep(stepResId = R.string.account_cancellation_instructions_tap_on_name)
        InstructionStepWithBoldText(stepResId = R.string.account_cancellation_instructions_select_subscriptions)
        GenericInstructionStep(stepResId = R.string.account_cancellation_instructions_select_mega_subscription)
        InstructionStepWithBoldText(stepResId = R.string.account_cancellation_instructions_open_select_cancel_subscription)


        MegaSpannedClickableText(
            value = stringResource(id = R.string.account_cancellation_instructions_detailed_instructions),
            styles = hashMapOf(
                SpanIndicator('A') to MegaSpanStyleWithAnnotation(
                    megaSpanStyle = MegaSpanStyle(
                        SpanStyle(textDecoration = TextDecoration.None),
                        color = TextColor.Accent,
                    ),
                    annotation = APPLE_SUPPORT_URL
                )
            ),
            onAnnotationClick = onCancelSubsFromOtherDeviceClicked,
            baseStyle = MaterialTheme.typography.subtitle1,
            color = TextColor.Primary,
            modifier = Modifier
                .padding(top = 20.dp)
                .testTag(APPLE_INSTRUCTIONS_DETAILED_INSTRUCTIONS_TEST_TAG),

            )
    }
}

@CombinedThemePreviews
@Composable
private fun AppleInstructionsViewPreview() {
    OriginalTempTheme(isDark = isSystemInDarkTheme()) {
        AppleInstructionsView(
            onCancelSubsFromOtherDeviceClicked = {},
        )
    }
}

internal const val APPLE_INSTRUCTIONS_VIEW_TEST_TAG = "apple_instructions"
internal const val APPLE_INSTRUCTIONS_TITLE_TEST_TAG = "apple_instructions:title_text"
internal const val APPLE_INSTRUCTIONS_SUBTITLE_TEST_TAG = "apple_instructions:subtitle_text"
internal const val APPLE_INSTRUCTIONS_HEADER_TEST_TAG =
    "apple_instructions:instructions_header_text"
internal const val APPLE_INSTRUCTIONS_DETAILED_INSTRUCTIONS_TEST_TAG =
    "apple_instructions:detailed_instructions_text"