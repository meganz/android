package mega.privacy.android.shared.original.core.ui.controls.layouts

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection

/**
 * Adds navigation bar padding to the start and end of the modifier.
 * This ensure content is not obscured by the 3-button navigation bar in landscape mode
 */
@Composable
fun Modifier.navigationBarsLandscapePadding(): Modifier {
    val navPadding = WindowInsets.navigationBars.asPaddingValues()
    val layoutDirection = LocalLayoutDirection.current
    return this.padding(
        start = navPadding.calculateStartPadding(layoutDirection),
        end = navPadding.calculateEndPadding(layoutDirection)
    )
}