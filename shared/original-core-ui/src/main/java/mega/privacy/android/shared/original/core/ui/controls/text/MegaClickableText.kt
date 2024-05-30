package mega.privacy.android.shared.original.core.ui.controls.text

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.text.ClickableText
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.AnnotatedString
import mega.privacy.android.shared.original.core.ui.preview.CombinedThemePreviews
import mega.privacy.android.shared.original.core.ui.theme.MegaOriginalTheme
import mega.privacy.android.shared.original.core.ui.theme.OriginalTempTheme

/**
 * Clickable text
 *
 * @param text    Text value
 * @param onClick Action to be performed when the text is clicked
 */
@Composable
fun MegaClickableText(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    ClickableText(
        modifier = modifier,
        text = AnnotatedString(text),
        onClick = {
            onClick()
        },
        style = MaterialTheme.typography.subtitle1.copy(
            color = MegaOriginalTheme.colors.text.accent,
        ),
    )
}

@CombinedThemePreviews
@Composable
private fun MegaClickableTextPreview() {
    OriginalTempTheme(isDark = isSystemInDarkTheme()) {
        MegaClickableText(
            text = "Click me",
            onClick = {},
        )
    }
}