package mega.privacy.android.feature.payment.components

import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import mega.android.core.ui.components.MegaText
import mega.android.core.ui.preview.CombinedThemePreviews
import mega.android.core.ui.theme.AndroidTheme
import mega.android.core.ui.theme.values.TextColor
import mega.android.core.ui.tokens.theme.DSTokens

/**
 * The "Best for you" badge on the quota plan cards.
 *
 * Core-ui's `Badge(BadgeType.Mega)` differs from the design in two ways that its API cannot
 * express: it pads its content by 2dp vertically and 4dp horizontally where the design calls for
 * 4dp and 10dp, and it rounds all four corners where the design rounds only the bottom end. The
 * padding sits inside the badge background, so a caller cannot widen it with a modifier — one
 * passed in lands outside the background and reads as an outer margin instead.
 *
 * The badge sits in the card's top-left corner, so its top-left curve comes from the card's own
 * clip and the remaining corners stay square. Same treatment as [DiscountBadge].
 */
@Composable
internal fun QuotaPlanBadge(
    text: String,
    modifier: Modifier = Modifier,
) {
    MegaText(
        text = text,
        style = MaterialTheme.typography.bodyMedium,
        textColor = TextColor.OnColor,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        modifier = modifier
            .background(
                color = DSTokens.colors.button.brand,
                shape = RoundedCornerShape(bottomEnd = 8.dp),
            )
            .padding(horizontal = 10.dp, vertical = 4.dp),
    )
}

@CombinedThemePreviews
@Composable
private fun QuotaPlanBadgePreview() {
    AndroidTheme(isSystemInDarkTheme()) {
        QuotaPlanBadge(text = "Best for you", modifier = Modifier.padding(16.dp))
    }
}
