package mega.privacy.android.core.ui.controls.chat

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import mega.privacy.android.core.ui.preview.CombinedThemePreviews
import mega.privacy.android.core.ui.theme.AndroidTheme
import mega.privacy.android.core.ui.theme.MegaTheme

/**
 * Chat meeting button
 *
 * @param text
 * @param onClick
 * @param modifier
 * @param enabled
 */
@Composable
fun ChatMeetingButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) = TextButton(
    modifier = modifier,
    onClick = onClick,
    enabled = enabled,
    shape = CircleShape,
    colors = ButtonDefaults.buttonColors(
        backgroundColor = MegaTheme.colors.background.inverse,
        contentColor = MegaTheme.colors.text.inverse,
        disabledBackgroundColor = MegaTheme.colors.button.disabled,
        disabledContentColor = MegaTheme.colors.text.disabled,
    ),
    contentPadding = PaddingValues(16.dp),
    elevation = ButtonDefaults.elevation(8.dp),
) {
    Text(
        text = text,
        style = MaterialTheme.typography.button
    )
}

@CombinedThemePreviews
@Composable
private fun ChatMeetingButtonPreview() {
    AndroidTheme(isDark = isSystemInDarkTheme()) {
        ChatMeetingButton(
            text = "Start meeting",
            onClick = {},
        )
    }
}