package mega.privacy.android.navigation.contract.extension

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.union
import androidx.compose.runtime.Composable
import androidx.compose.runtime.NonRestartableComposable
import mega.privacy.android.navigation.contract.state.LocalNavigationRailVisible

/**
 * Extension property to get system bars insets except bottom
 */
val WindowInsets.Companion.systemBarsIgnoringBottom
    @Composable @NonRestartableComposable get() = WindowInsets.systemBars.union(displayCutout).only(
        WindowInsetsSides.Top + WindowInsetsSides.Horizontal
    )

/**
 * Extension property to get system bars insets based on navigation rail visibility
 */
@Composable
fun WindowInsets.Companion.systemBarsWithRail(
    isNavigationRailVisible: Boolean = LocalNavigationRailVisible.current,
) = if (isNavigationRailVisible) {
    WindowInsets.systemBars.union(displayCutout).only(
        WindowInsetsSides.Vertical + WindowInsetsSides.End
    )
} else {
    WindowInsets.systemBarsIgnoringBottom
}