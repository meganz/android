package mega.privacy.android.shared.original.core.ui.controls.textfields

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import mega.privacy.android.shared.original.core.ui.preview.CombinedThemePreviews
import mega.privacy.android.shared.original.core.ui.theme.OriginalTempTheme
import mega.privacy.android.shared.original.core.ui.theme.MegaOriginalTheme

/**
 * Error text in text field.
 *
 * @param errorText Error text.
 * @param modifier  [Modifier]
 */
@Composable
fun ErrorTextTextField(
    errorText: String,
    modifier: Modifier = Modifier,
) {
    Text(
        modifier = modifier
            .fillMaxWidth()
            .padding(top = 4.dp),
        text = errorText,
        style = MaterialTheme.typography.caption.copy(color = MegaOriginalTheme.colors.text.error),
        textAlign = TextAlign.Start
    )
}

@CombinedThemePreviews
@Composable
private fun PreviewErrorTextTextField() {
    OriginalTempTheme(isDark = isSystemInDarkTheme()) {
        ErrorTextTextField(errorText = "Error text")
    }
}