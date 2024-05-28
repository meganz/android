package mega.privacy.android.shared.original.core.ui.controls.chat

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import androidx.compose.ui.unit.dp
import mega.privacy.android.core.R
import mega.privacy.android.shared.original.core.ui.controls.buttons.FloatingActionButtonStyle
import mega.privacy.android.shared.original.core.ui.controls.buttons.MegaFloatingActionButton
import mega.privacy.android.shared.original.core.ui.controls.text.MegaText
import mega.privacy.android.shared.original.core.ui.preview.CombinedThemePreviews
import mega.privacy.android.shared.original.core.ui.theme.OriginalTempTheme
import mega.privacy.android.shared.original.core.ui.theme.MegaOriginalTheme
import mega.privacy.android.shared.original.core.ui.theme.extensions.body4
import mega.privacy.android.shared.original.core.ui.theme.extensions.conditional
import mega.privacy.android.shared.original.core.ui.theme.values.TextColor


internal const val SCROLL_TO_BOTTOM_FAB_TEST_TAG = "scroll_to_bottom_fab:fab"

/**
 * Scroll to bottom fab
 *
 * @param modifier modifier
 * @param onClick click listener
 * @param unreadCount unread count
 */
@Composable
fun ScrollToBottomFab(
    unreadCount: Int,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    Box(modifier = modifier) {
        MegaFloatingActionButton(
            modifier = Modifier
                .conditional(unreadCount > 0) {
                    padding(top = 6.dp, start = 6.dp)
                }
                .testTag(SCROLL_TO_BOTTOM_FAB_TEST_TAG),
            onClick = onClick,
            style = FloatingActionButtonStyle.SmallWithoutElevation,
            backgroundColor = MegaOriginalTheme.colors.icon.primary
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .border(1.dp, MegaOriginalTheme.colors.border.subtle, CircleShape)
            ) {
                Icon(
                    modifier = modifier.align(Alignment.Center),
                    painter = painterResource(id = R.drawable.ic_arrow_down),
                    contentDescription = "Icon Arrow Down",
                    tint = MegaOriginalTheme.colors.icon.inverse
                )
            }
        }
        if (unreadCount > 0) {
            MegaText(
                modifier = Modifier
                    .background(MegaOriginalTheme.colors.icon.primary, RoundedCornerShape(20.dp))
                    .border(1.dp, MegaOriginalTheme.colors.border.subtle, RoundedCornerShape(20.dp))
                    .padding(horizontal = 6.dp, vertical = 3.dp),
                text = unreadCount.formatUnreadCount(), textColor = TextColor.Inverse,
                style = MaterialTheme.typography.body4
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
    OriginalTempTheme(isDark = isSystemInDarkTheme()) {
        ScrollToBottomFab(onClick = {}, unreadCount = unreadCount)
    }
}

private class UnreadCountProvider : PreviewParameterProvider<Int> {
    override val values: Sequence<Int> = sequenceOf(0, 1, 99, 199)
}