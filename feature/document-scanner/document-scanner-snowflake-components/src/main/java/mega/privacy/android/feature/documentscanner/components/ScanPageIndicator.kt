package mega.privacy.android.feature.documentscanner.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import mega.android.core.ui.components.MegaText
import mega.android.core.ui.theme.AppTheme
import mega.android.core.ui.theme.values.TextColor
import mega.android.core.ui.tokens.theme.DSTokens

/**
 * A small "current / total" pill shown over the page preview so the user knows
 * which page they are looking at.
 *
 * @param currentPage 1-based index of the page in view.
 * @param totalPages Total number of pages.
 * @param modifier Modifier (the caller positions it over the preview).
 */
@Composable
fun ScanPageIndicator(
    currentPage: Int,
    totalPages: Int,
    modifier: Modifier = Modifier,
) {
    MegaText(
        text = "$currentPage / $totalPages",
        textColor = TextColor.OnColor,
        style = AppTheme.typography.labelMedium,
        modifier = modifier
            .clip(RoundedCornerShape(50))
            .background(DSTokens.colors.background.blur)
            .padding(horizontal = 12.dp, vertical = 4.dp),
    )
}
