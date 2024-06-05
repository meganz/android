package mega.privacy.android.shared.original.core.ui.controls.other

import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import mega.privacy.android.shared.original.core.ui.controls.text.MegaText
import mega.privacy.android.shared.original.core.ui.preview.CombinedThemePreviews
import mega.privacy.android.shared.original.core.ui.theme.MegaOriginalTheme
import mega.privacy.android.shared.original.core.ui.theme.OriginalTempThemeForPreviews
import mega.privacy.android.shared.original.core.ui.theme.values.TextColor

/**
 * Small tooltip to show current position on Fast scroll
 */
@Composable
fun Counter(
    text: String,
    modifier: Modifier = Modifier,
) = Surface(
    modifier = modifier
        .height(28.dp)
        .widthIn(min = 28.dp),
    shape = CircleShape,
    color = MegaOriginalTheme.colors.background.surface1,
    elevation = 8.dp
) {
    MegaText(
        modifier = Modifier
            .testTag(COUNTER_TEXT_TAG)
            .fillMaxHeight()
            .padding(start = 11.dp, end = 11.dp, top = 3.dp),
        text = text,
        textColor = TextColor.Secondary,
    )
}

internal const val COUNTER_TEXT_TAG = "counter:text_text"

@CombinedThemePreviews
@Composable
private fun CounterPreview() {
    OriginalTempThemeForPreviews {
        Counter("1/10")
    }
}