package mega.privacy.android.feature.clouddrive.presentation.clouddrive.view

import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import mega.android.core.ui.modifiers.ScrollToHideBehavior

/**
 * Tracks the scroll direction reported by [scrollToHideBehavior] and turns it into a visibility
 * flag for content that hides while scrolling down and comes back on scroll up.
 *
 * [scrollThreshold] keeps small scrolls and accidental touches from flipping it, matching the
 * threshold the floating action button uses so both react to the same gesture.
 *
 * @param scrollToHideBehavior the behavior provided by the enclosing scaffold, or null to stay visible
 * @param scrollThreshold the minimum scroll distance required to change the visibility
 */
@Composable
internal fun rememberScrollUpVisibility(
    scrollToHideBehavior: ScrollToHideBehavior?,
    scrollThreshold: Dp = 10.dp,
): Boolean {
    val threshold = with(LocalDensity.current) { scrollThreshold.toPx() }
    var visible by rememberSaveable { mutableStateOf(true) }
    val shouldUpdateVisibility by remember(scrollToHideBehavior, threshold) {
        derivedStateOf {
            val offset = scrollToHideBehavior?.scrollDirectionOffset ?: return@derivedStateOf null
            when {
                offset <= -threshold -> false
                offset >= threshold -> true
                else -> null
            }
        }
    }
    shouldUpdateVisibility?.let { visible = it }
    return visible
}
