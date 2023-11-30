package mega.privacy.android.legacy.core.ui.controls.chips

import androidx.annotation.DrawableRes
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.LocalTextSelectionColors
import androidx.compose.foundation.text.selection.TextSelectionColors
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.dp
import mega.privacy.android.shared.theme.MegaAppTheme
import mega.privacy.android.core.ui.preview.CombinedThemePreviews
import mega.privacy.android.core.ui.preview.TextFieldProvider
import mega.privacy.android.core.ui.preview.TextFieldState
import mega.privacy.android.core.ui.theme.extensions.textColorSecondary

/**
 * Text button with icon chip
 *
 * @param text          The text in TextButton
 * @param onClick       Lambda to receive clicks on this button
 * @param modifier
 * @param isChecked     True, if it's checked. False, if not
 * @param iconId        Icon id resource
 */
@Composable
fun TextButtonWithIconChipForSearch(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    isChecked: Boolean = false,
    @DrawableRes iconId: Int? = null,
) = Box {

    val customTextSelectionColors = TextSelectionColors(
        handleColor = MaterialTheme.colors.secondary,
        backgroundColor = MaterialTheme.colors.secondary
    )
    CompositionLocalProvider(LocalTextSelectionColors provides customTextSelectionColors) {

        TextButton(
            modifier = modifier,
            onClick = onClick,
            shape = RoundedCornerShape(size = 8.dp),
            border = BorderStroke(
                1.dp,
                if (isChecked) MaterialTheme.colors.secondary else MaterialTheme.colors.textColorSecondary
            ),
            enabled = true,
            colors = if (isChecked) colorsChecked() else colorsUnChecked(),
            contentPadding = PaddingValues(
                start = 10.dp,
                end = 16.dp,
                bottom = 6.dp,
                top = 6.dp
            ),
            elevation = ButtonDefaults.elevation(0.dp),
        ) {
            if (isChecked) {
                Row {
                    iconId?.let { id ->
                        Icon(
                            imageVector = ImageVector.vectorResource(id = id),
                            contentDescription = "Check",
                            modifier = Modifier
                                .width(18.dp)
                                .height(18.dp),
                            tint = MaterialTheme.colors.onSecondary
                        )
                    }

                    Text(
                        text = text,
                        modifier = modifier.padding(start = 10.dp),
                        style = MaterialTheme.typography.button.copy(color = MaterialTheme.colors.onSecondary)
                    )
                }
            } else {
                Text(
                    text = text,
                    modifier = modifier.padding(start = 6.dp),
                    style = MaterialTheme.typography.button.copy(color = MaterialTheme.colors.textColorSecondary),
                )
            }
        }
    }
}

@Composable
private fun colorsChecked() = ButtonDefaults.buttonColors(
    backgroundColor = MaterialTheme.colors.secondary,
    contentColor = MaterialTheme.colors.secondary,
    disabledContentColor = MaterialTheme.colors.secondary,
    disabledBackgroundColor = MaterialTheme.colors.secondary,
)


@Composable
private fun colorsUnChecked() = ButtonDefaults.buttonColors(
    backgroundColor = MaterialTheme.colors.onSecondary,
    contentColor = MaterialTheme.colors.onSecondary,
    disabledContentColor = MaterialTheme.colors.onSecondary,
    disabledBackgroundColor = MaterialTheme.colors.onSecondary,
)


@CombinedThemePreviews
@Composable
private fun PreviewTextButtonWithIconChip(
    @PreviewParameter(TextFieldProvider::class) state: TextFieldState,
) {
    MegaAppTheme(isDark = isSystemInDarkTheme()) {
        TextButtonWithIconChip(
            onClick = { },
            text = "weekdays",
            modifier = Modifier,
            isChecked = false,
        )
    }
}
