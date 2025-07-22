package mega.privacy.mobile.navigation.snowflake.model

import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.Easing
import androidx.compose.animation.core.tween

/**
 * Configuration for navigation animation parameters
 */
data class NavigationAnimationConfig(
    val durationMillis: Int,
    val easing: Easing,
) {
    /**
     * Creates an AnimationSpec for the navigation animations
     */
    fun createAnimationSpec(): AnimationSpec<Float> = tween(
        durationMillis = durationMillis,
        easing = easing,
        delayMillis = 0
    )
}