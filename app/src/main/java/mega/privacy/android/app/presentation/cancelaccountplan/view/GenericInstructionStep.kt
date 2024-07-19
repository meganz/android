package mega.privacy.android.app.presentation.cancelaccountplan.view

import androidx.compose.foundation.layout.padding
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextIndent
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import mega.privacy.android.shared.original.core.ui.controls.text.MegaText
import mega.privacy.android.shared.original.core.ui.theme.values.TextColor

/**
 * Composable function to display a generic instruction step
 */
@Composable
internal fun GenericInstructionStep(stepResId: Int) {
    MegaText(
        text = stringResource(id = stepResId),
        textColor = TextColor.Secondary,
        style = MaterialTheme.typography.subtitle1.copy(textIndent = TextIndent(restLine = 18.sp)),
        textAlign = TextAlign.Start,
        modifier = Modifier
            .padding(top = 4.dp, start = 4.dp)
            .testTag(GENERIC_INSTRUCTIONS_STEP_TEST_TAG),
    )
}

internal const val GENERIC_INSTRUCTIONS_STEP_TEST_TAG =
    "generic_instructions:instruction_step_text"