package mega.privacy.android.navigation.contract.transition

import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith

/**
 * Slide transition for forward navigation (entering screen slides in from right)
 */
val slideForwardTransition = slideInHorizontally(
    initialOffsetX = { it }
) togetherWith slideOutHorizontally(
    targetOffsetX = { -it }
)

/**
 * Slide transition for backward navigation (entering screen slides in from left)
 */
val slideBackwardTransition = slideInHorizontally(
    initialOffsetX = { -it }
) togetherWith slideOutHorizontally(
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

private const val FADE_ANIM_DURATION_MS = 700