package mega.privacy.android.navigation.contract.extension

import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.navigation3.ui.NavDisplay

/**
 * Slide transition for forward navigation (entering screen slides in from right)
 */
val slideForwardTransition = slideInHorizontally(
    animationSpec = tween(durationMillis = SLIDE_ANIM_DURATION_MS),
    initialOffsetX = { it }
) togetherWith slideOutHorizontally(
    animationSpec = tween(durationMillis = SLIDE_ANIM_DURATION_MS),
    targetOffsetX = { -it }
)

/**
 * Slide transition for backward navigation (entering screen slides in from left)
 */
val slideBackwardTransition = slideInHorizontally(
    animationSpec = tween(durationMillis = SLIDE_ANIM_DURATION_MS),
    initialOffsetX = { -it }
) togetherWith slideOutHorizontally(
    animationSpec = tween(durationMillis = SLIDE_ANIM_DURATION_MS),
    targetOffsetX = { it }
)

/**
 * Screen exit transition with fade in animation
 */
val fadeTransition = fadeIn(
    animationSpec = tween(FADE_ANIM_DURATION_MS)
) togetherWith fadeOut(
    animationSpec = tween(FADE_ANIM_DURATION_MS)
)

/**
 * Metadata for slide transitions, used in NavEntry
 */
val slideTransitionMetadata = NavDisplay.transitionSpec {
    slideForwardTransition
} + NavDisplay.popTransitionSpec {
    slideBackwardTransition
} + NavDisplay.predictivePopTransitionSpec {
    slideBackwardTransition
}

private const val SLIDE_ANIM_DURATION_MS = 600
private const val FADE_ANIM_DURATION_MS = 300