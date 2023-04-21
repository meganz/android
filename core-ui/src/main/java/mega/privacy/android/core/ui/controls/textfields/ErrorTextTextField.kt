package mega.privacy.android.core.ui.controls.textfields

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import mega.privacy.android.core.ui.preview.CombinedThemePreviews
import mega.privacy.android.core.ui.theme.AndroidTheme

/**
 * Error text in text field
 *
 * @param errorText   String
 */
@Composable
fun ErrorTextTextField(
    errorText: String, modifier: Modifier = Modifier,
) {
    Text(
        modifier = modifier
            .fillMaxWidth()
            .padding(top = 4.dp),
        text = errorText,
        style = MaterialTheme.typography.caption.copy(color = MaterialTheme.colors.error),
        textAlign = TextAlign.Start
    )
}

@CombinedThemePreviews
@Composable
private fun PreviewErrorTextTextField() {
    AndroidTheme(isDark = isSystemInDarkTheme()) {
        ErrorTextTextField(errorText = "Error text")
    }
}