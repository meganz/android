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
import androidx.compose.ui.text.style.TextIndent
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import mega.android.core.ui.theme.values.TextColor
import mega.privacy.android.app.presentation.cancelaccountplan.view.GenericInstructionStep
import mega.privacy.android.app.presentation.cancelaccountplan.view.InstructionStepWithBoldText
import mega.privacy.android.app.utils.ConstantsUrl.megaUrl
import mega.privacy.android.app.utils.DomainNameFacade
import mega.privacy.android.shared.original.core.ui.controls.text.MegaSpannedClickableText
import mega.privacy.android.shared.original.core.ui.controls.text.MegaText
import mega.privacy.android.shared.original.core.ui.model.MegaSpanStyle
import mega.privacy.android.shared.original.core.ui.model.MegaSpanStyleWithAnnotation
import mega.privacy.android.shared.original.core.ui.model.SpanIndicator
import mega.privacy.android.shared.original.core.ui.preview.CombinedThemePreviews
import mega.privacy.android.shared.original.core.ui.theme.OriginalTheme
import mega.privacy.android.shared.original.core.ui.theme.extensions.h6Medium
import mega.privacy.android.shared.original.core.ui.theme.extensions.subtitle1medium
import mega.privacy.android.shared.resources.R

/**
 * Composable function to display the instructions to cancel the subscription on the web
 */
@Composable
internal fun WebCancellationInstructionsView(
    onMegaUrlClicked: (url: String) -> Unit,
) {
    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .verticalScroll(scrollState)
            .padding(20.dp)
            .testTag(WEB_CANCELLATION_INSTRUCTIONS_VIEW_TEST_TAG)
    ) {
        MegaText(
            text = stringResource(id = R.string.account_cancellation_instructions_cancellation_web_browser_needed),
            textColor = TextColor.Primary,
            style = MaterialTheme.typography.h6Medium,
            modifier = Modifier
                .padding(top = 10.dp)
        )
        MegaText(
            text = stringResource(id = R.string.account_cancellation_instructions_web_browser_description),
            textColor = TextColor.Secondary,
            style = MaterialTheme.typography.subtitle1,
            textAlign = TextAlign.Start,
            modifier = Modifier
                .padding(top = 20.dp)
        )

        //computer instructions
        MegaText(
            text = stringResource(id = R.string.account_cancellation_instructions_on_computer),
            textColor = TextColor.Primary,
            style = MaterialTheme.typography.subtitle1medium,
            modifier = Modifier
                .padding(top = 30.dp, end = 8.dp)
        )

        MegaSpannedClickableText(
            value = stringResource(
                id = R.string.account_cancellation_instructions_visit_website_domain_placeholder,
                DomainNameFacade.getDomainName()
            ),
            styles = hashMapOf(
                SpanIndicator('A') to MegaSpanStyleWithAnnotation(
                    megaSpanStyle = MegaSpanStyle(color = TextColor.Accent),
                    annotation = megaUrl(DomainNameFacade.getDomainName())
                )
            ),
            onAnnotationClick = onMegaUrlClicked,
            baseStyle = MaterialTheme.typography.subtitle1.copy(textIndent = TextIndent(restLine = 18.sp)),
            color = TextColor.Secondary,
            modifier = Modifier
                .padding(top = 4.dp, start = 4.dp)
                .testTag(WEB_CANCELLATION_INSTRUCTIONS_COMPUTER_STEP_WITH_URL_TEST_TAG),
        )

        GenericInstructionStep(stepResId = R.string.account_cancellation_instructions_login_account)
        GenericInstructionStep(stepResId = R.string.account_cancellation_instructions_click_main_menu)
        InstructionStepWithBoldText(stepResId = R.string.account_cancellation_instructions_click_settings)
        InstructionStepWithBoldText(stepResId = R.string.account_cancellation_instructions_click_plan)
        InstructionStepWithBoldText(stepResId = R.string.account_cancellation_instructions_click_cancel_subscription)

        //mobile device instructions
        MegaText(
            text = stringResource(id = R.string.account_cancellation_instructions_on_mobile_device),
            textColor = TextColor.Primary,
            style = MaterialTheme.typography.subtitle1medium,
            modifier = Modifier
                .padding(top = 30.dp, end = 8.dp)
        )

        MegaSpannedClickableText(
            value = stringResource(
                id = R.string.account_cancellation_instructions_visit_website_domain_placeholder,
                DomainNameFacade.getDomainName()
            ),
            styles = hashMapOf(
                SpanIndicator('A') to MegaSpanStyleWithAnnotation(
                    megaSpanStyle = MegaSpanStyle(
                        SpanStyle(textDecoration = TextDecoration.None),
                        color = TextColor.Accent,
                    ),
                    annotation = megaUrl(DomainNameFacade.getDomainName())
                )
            ),
            onAnnotationClick = onMegaUrlClicked,
            baseStyle = MaterialTheme.typography.subtitle1.copy(textIndent = TextIndent(restLine = 18.sp)),
            color = TextColor.Secondary,
            modifier = Modifier
                .padding(top = 4.dp, start = 4.dp)
                .testTag(WEB_CANCELLATION_INSTRUCTIONS_MOBILE_STEP_WITH_URL_TEST_TAG),
        )
        GenericInstructionStep(stepResId = R.string.account_cancellation_instructions_login_account)
        GenericInstructionStep(stepResId = R.string.account_cancellation_instructions_tap_avatar)
        InstructionStepWithBoldText(stepResId = R.string.account_cancellation_instructions_tap_cancel_subscription)
    }
}


@CombinedThemePreviews
@Composable
private fun WebCancellationInstructionsViewPreview() {
    OriginalTheme(isDark = isSystemInDarkTheme()) {
        WebCancellationInstructionsView(
            onMegaUrlClicked = {},
        )
    }
}

internal const val WEB_CANCELLATION_INSTRUCTIONS_VIEW_TEST_TAG = "web_cancellation_instructions"
internal const val WEB_CANCELLATION_INSTRUCTIONS_COMPUTER_STEP_WITH_URL_TEST_TAG =
    "web_cancellation_instructions:instruction_computer_step_with_url_text"
internal const val WEB_CANCELLATION_INSTRUCTIONS_MOBILE_STEP_WITH_URL_TEST_TAG =
    "web_cancellation_instructions:instruction_mobile_step_with_url_text"

