package mega.privacy.android.shared.original.core.ui.controls.chat

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
import mega.privacy.android.shared.original.core.ui.preview.CombinedThemePreviews
import mega.privacy.android.shared.original.core.ui.theme.OriginalTempTheme
import mega.privacy.android.shared.original.core.ui.theme.MegaOriginalTheme

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
        backgroundColor = MegaOriginalTheme.colors.background.inverse,
        contentColor = MegaOriginalTheme.colors.text.inverse,
        disabledBackgroundColor = MegaOriginalTheme.colors.button.disabled,
        disabledContentColor = MegaOriginalTheme.colors.text.disabled,
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
    OriginalTempTheme(isDark = isSystemInDarkTheme()) {
        ChatMeetingButton(
            text = "Start meeting",
            onClick = {},
        )
    }
}