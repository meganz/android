package mega.privacy.android.app.presentation.cancelaccountplan.view

import androidx.compose.foundation.layout.padding
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextIndent
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import mega.privacy.android.shared.original.core.ui.controls.text.MegaSpannedText
import mega.privacy.android.shared.original.core.ui.model.MegaSpanStyle
import mega.privacy.android.shared.original.core.ui.model.SpanIndicator
import mega.privacy.android.shared.original.core.ui.theme.values.TextColor

/**
 * Composable function to display a generic instruction step with highlighted bold text
 */
@Composable
internal fun InstructionStepWithBoldText(stepResId: Int) {
    MegaSpannedText(
        value = stringResource(id = stepResId),
        styles = hashMapOf(
            SpanIndicator('A') to MegaSpanStyle(
                spanStyle = SpanStyle(fontWeight = FontWeight.Bold),
            )
        ),
        baseStyle = MaterialTheme.typography.subtitle1.copy(textIndent = TextIndent(restLine = 18.sp)),
        color = TextColor.Secondary,
        modifier = Modifier
            .padding(top = 4.dp, start = 4.dp)
            .testTag(INSTRUCTIONS_STEP_WITH_BOLD_TEXT_TEST_TAG),
    )
}

internal const val INSTRUCTIONS_STEP_WITH_BOLD_TEXT_TEST_TAG =
    "instruction_step_with_bold_text:instruction_step_text"