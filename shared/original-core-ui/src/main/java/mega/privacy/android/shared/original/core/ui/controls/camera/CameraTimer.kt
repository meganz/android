package mega.privacy.android.shared.original.core.ui.controls.camera

import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import mega.privacy.android.shared.original.core.ui.controls.text.MegaText
import mega.privacy.android.shared.original.core.ui.preview.CombinedThemePreviews
import mega.privacy.android.shared.original.core.ui.theme.OriginalTempTheme
import mega.privacy.android.shared.original.core.ui.theme.MegaOriginalTheme
import mega.privacy.android.shared.original.core.ui.theme.values.TextColor

/**
 * Camera timer
 *
 * @param modifier Modifier
 * @param formattedTime Formatted time
 */
@Composable
fun CameraTimer(
    formattedTime: String,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .background(
                color = MegaOriginalTheme.colors.background.surface1.copy(alpha = 0.9f),
                shape = CircleShape
            )
            .padding(horizontal = 12.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .background(color = MegaOriginalTheme.colors.components.interactive, shape = CircleShape)
        )
        MegaText(
            text = formattedTime,
            textColor = TextColor.Primary,
            style = MaterialTheme.typography.subtitle1
        )
    }
}

@CombinedThemePreviews
@Composable
private fun CameraTimerPreview() {
    OriginalTempTheme(isDark = isSystemInDarkTheme()) {
        CameraTimer(formattedTime = "00:00:00")
    }
}