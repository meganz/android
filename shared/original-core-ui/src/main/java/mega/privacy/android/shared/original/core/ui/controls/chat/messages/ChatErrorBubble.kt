package mega.privacy.android.shared.original.core.ui.controls.chat.messages

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.unit.dp
import mega.privacy.android.shared.original.core.ui.controls.text.MegaText
import mega.privacy.android.shared.original.core.ui.preview.CombinedThemePreviews
import mega.privacy.android.shared.original.core.ui.theme.OriginalTempTheme
import mega.privacy.android.shared.original.core.ui.theme.MegaOriginalTheme
import mega.privacy.android.shared.original.core.ui.theme.values.TextColor

/**
 * Chat error bubble.
 */
@Composable
fun ChatErrorBubble(
    errorText: String,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .border(
                BorderStroke(1.dp, SolidColor(MegaOriginalTheme.colors.text.error)),
                RoundedCornerShape(12.dp)
            ),
    ) {
        MegaText(
            text = errorText,
            textColor = TextColor.Error,
            style = MaterialTheme.typography.subtitle2,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
        )
    }
}

@CombinedThemePreviews
@Composable
private fun ChatErrorBubblePreview() {
    OriginalTempTheme(isDark = isSystemInDarkTheme()) {
        ChatErrorBubble(errorText = "Invalid message format")
    }
}