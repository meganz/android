package mega.privacy.android.navigation.contract.extension

import androidx.compose.animation.ContentTransform
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
val slideForwardTransition = slideInHorizontally(initialOffsetX = { it }) togetherWith
        slideOutHorizontally(targetOffsetX = { -it })

/**
 * Slide transition for backward navigation (entering screen slides in from left)
 */
val slideBackwardTransition = slideInHorizontally(initialOffsetX = { -it }) togetherWith
        slideOutHorizontally(targetOffsetX = { it })

/**
 * Screen exit transition with fade in animation
 */
val fadeTransition = ContentTransform(
    fadeIn(animationSpec = tween(300)),
    fadeOut(animationSpec = tween(300)),
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