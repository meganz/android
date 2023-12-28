package mega.privacy.android.core.ui.controls.buttons

import androidx.appcompat.R
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import mega.privacy.android.core.ui.preview.CombinedThemePreviews
import mega.privacy.android.core.ui.theme.AndroidTheme
import mega.privacy.android.core.ui.theme.MegaTheme

/**
 * Text button
 */
@Composable
fun TextMegaButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    contentPadding: PaddingValues = PaddingValues(horizontal = 8.dp, vertical = 10.dp),
) = TextButton(
    modifier = modifier,
    onClick = onClick,
    enabled = enabled,
    colors = ButtonDefaults.buttonColors(
        backgroundColor = Color.Transparent,
        contentColor = MegaTheme.colors.text.accent,
        disabledBackgroundColor = Color.Transparent,
        disabledContentColor = MegaTheme.colors.text.disabled,
    ),
    contentPadding = contentPadding
) {
    Text(
        text = text,
        style = MaterialTheme.typography.button
    )
}

/**
 * Text button
 */
@Composable
fun TextMegaButton(
    textId: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    contentPadding: PaddingValues = PaddingValues(horizontal = 8.dp, vertical = 10.dp),
) = TextMegaButton(
    text = stringResource(id = textId),
    onClick = onClick,
    modifier = modifier,
    enabled = enabled,
    contentPadding = contentPadding,
)

@CombinedThemePreviews
@Composable
private fun PreviewTextMegaButton() {
    AndroidTheme(isDark = isSystemInDarkTheme()) {
        Column(
            modifier = Modifier.padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            listOf(true, false).forEach {
                Text(
                    style = MaterialTheme.typography.caption.copy(color = MaterialTheme.colors.onSurface),
                    text = if (it) "Enabled" else "Disabled",
                )
                TextMegaButton(
                    textId = R.string.search_menu_title,
                    onClick = {},
                    enabled = it
                )
            }
        }
    }
}