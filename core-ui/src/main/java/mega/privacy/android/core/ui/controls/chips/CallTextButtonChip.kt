package mega.privacy.android.core.ui.controls.chips

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.LocalTextSelectionColors
import androidx.compose.foundation.text.selection.TextSelectionColors
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.dp
import mega.privacy.android.core.ui.preview.CombinedThemePreviews
import mega.privacy.android.core.ui.preview.TextFieldProvider
import mega.privacy.android.core.ui.preview.TextFieldState
import mega.privacy.android.core.ui.theme.AndroidTheme
import mega.privacy.android.core.ui.theme.extensions.grey_alpha_012_white_alpha_012
import mega.privacy.android.core.ui.theme.extensions.grey_alpha_054_white_alpha_054
import mega.privacy.android.core.ui.theme.extensions.grey_alpha_087_white
import mega.privacy.android.core.ui.theme.extensions.white_alpha_087_grey_alpha_087
import mega.privacy.android.core.ui.theme.extensions.white_grey_800

/**
 * Call Text button chip
 *
 * @param text          The text in TextButton
 * @param onClick       Lambda to receive clicks on this button
 * @param modifier
 * @param isChecked     True, if it's checked. False, if not
 */
@Composable
fun CallTextButtonChip(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    isChecked: Boolean = true,
) = Box {

    val customTextSelectionColors = TextSelectionColors(
        handleColor = MaterialTheme.colors.secondary,
        backgroundColor = MaterialTheme.colors.secondary
    )
    CompositionLocalProvider(LocalTextSelectionColors provides customTextSelectionColors) {
        TextButton(
            modifier = modifier,
            onClick = onClick,
            shape = RoundedCornerShape(size = 18.dp),
            border = BorderStroke(
                1.dp,
                if (isChecked) MaterialTheme.colors.grey_alpha_087_white else MaterialTheme.colors.grey_alpha_012_white_alpha_012
            ),
            enabled = true,
            colors = if (isChecked) colorsChecked() else colorsUnChecked(),
            contentPadding = PaddingValues(
                start = 12.dp,
                end = 12.dp,
                bottom = 8.dp,
                top = 8.dp
            ),
            elevation = ButtonDefaults.elevation(0.dp),
        ) {
            Text(
                text = text,
                style = MaterialTheme.typography.body2.copy(
                    textAlign = TextAlign.Center, color =
                    if (isChecked)
                        MaterialTheme.colors.white_alpha_087_grey_alpha_087
                    else
                        MaterialTheme.colors.grey_alpha_054_white_alpha_054
                )
            )
        }
    }
}

@Composable
private fun colorsChecked() = ButtonDefaults.buttonColors(
    backgroundColor = MaterialTheme.colors.grey_alpha_087_white,
    contentColor = MaterialTheme.colors.grey_alpha_087_white,
    disabledContentColor = MaterialTheme.colors.grey_alpha_087_white,
    disabledBackgroundColor = MaterialTheme.colors.grey_alpha_087_white,
)


@Composable
private fun colorsUnChecked() = ButtonDefaults.buttonColors(
    backgroundColor = MaterialTheme.colors.white_grey_800,
    contentColor = MaterialTheme.colors.white_grey_800,
    disabledContentColor = MaterialTheme.colors.white_grey_800,
    disabledBackgroundColor = MaterialTheme.colors.white_grey_800,
)


@CombinedThemePreviews
@Composable
private fun PreviewCallTextButtonChipChecked(
    @PreviewParameter(TextFieldProvider::class) state: TextFieldState,
) {
    AndroidTheme(isDark = true) {
        CallTextButtonChip(
            onClick = { },
            text = "Waiting room",
            modifier = Modifier,
            isChecked = true,
        )
    }
}

@CombinedThemePreviews
@Composable
private fun PreviewCallTextButtonChipUnChecked(
    @PreviewParameter(TextFieldProvider::class) state: TextFieldState,
) {
    AndroidTheme(isDark = true) {
        CallTextButtonChip(
            onClick = { },
            text = "In call",
            modifier = Modifier,
            isChecked = false,
        )
    }
}