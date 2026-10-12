package mega.privacy.android.feature.chat.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import androidx.compose.ui.unit.dp
import mega.android.core.ui.components.MegaText
import mega.android.core.ui.components.image.MegaIcon
import mega.android.core.ui.modifiers.conditional
import mega.android.core.ui.preview.CombinedThemePreviews
import mega.android.core.ui.theme.AndroidThemeForPreviews
import mega.android.core.ui.theme.AppTheme
import mega.android.core.ui.theme.values.IconColor
import mega.android.core.ui.theme.values.TextColor
import mega.android.core.ui.tokens.theme.DSTokens
import mega.privacy.android.icon.pack.R as IconPackR

/**
 * Small circular button that scrolls the chat room to the latest message.
 *
 * When [unreadCount] is greater than zero, a badge with the count shows on the top-start corner.
 * Counts above 99 show as "+99".
 *
 * @param unreadCount Number of unread messages below the visible area.
 * @param onClick Invoked when the button is tapped.
 * @param modifier [Modifier]
 * @param contentDescription Accessibility label of the button.
 */
@Composable
fun ScrollToBottomFab(
    unreadCount: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    contentDescription: String? = null,
) {
    Box(modifier = modifier) {
        Box(
            modifier = Modifier
                .conditional(unreadCount > 0) {
                    padding(top = 6.dp, start = 6.dp)
                }
                .size(40.dp)
                .clip(CircleShape)
                .background(DSTokens.colors.icon.primary)
                .border(1.dp, DSTokens.colors.border.subtle, CircleShape)
                .clickable(role = Role.Button, onClick = onClick)
                .testTag(SCROLL_TO_BOTTOM_FAB_TAG),
            contentAlignment = Alignment.Center,
        ) {
            MegaIcon(
                painter = painterResource(IconPackR.drawable.ic_arrow_down_medium_thin_outline),
                tint = IconColor.Inverse,
                contentDescription = contentDescription,
                modifier = Modifier.size(24.dp),
            )
        }
        if (unreadCount > 0) {
            MegaText(
                text = unreadCount.formatUnreadCount(),
                textColor = TextColor.Inverse,
                style = AppTheme.typography.labelSmall,
                modifier = Modifier
                    .background(DSTokens.colors.icon.primary, RoundedCornerShape(20.dp))
                    .border(1.dp, DSTokens.colors.border.subtle, RoundedCornerShape(20.dp))
                    .padding(horizontal = 6.dp, vertical = 3.dp)
                    .testTag(SCROLL_TO_BOTTOM_FAB_BADGE_TAG),
            )
        }
    }
}

private fun Int.formatUnreadCount(): String = if (this > 99) "+99" else "$this"

@CombinedThemePreviews
@Composable
private fun ScrollToBottomFabPreview(
    @PreviewParameter(UnreadCountProvider::class) unreadCount: Int,
) {
    AndroidThemeForPreviews {
        ScrollToBottomFab(unreadCount = unreadCount, onClick = {})
    }
}

private class UnreadCountProvider : PreviewParameterProvider<Int> {
    override val values: Sequence<Int> = sequenceOf(0, 1, 99, 199)
}

/**
 * Test tag for the scroll-to-bottom button.
 */
const val SCROLL_TO_BOTTOM_FAB_TAG = "scroll_to_bottom_fab:fab"

/**
 * Test tag for the unread count badge of the scroll-to-bottom button.
 */
const val SCROLL_TO_BOTTOM_FAB_BADGE_TAG = "scroll_to_bottom_fab:badge"
