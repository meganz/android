package mega.privacy.android.core.sharedcomponents.extension

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.systemBars
import androidx.compose.runtime.Composable
import androidx.compose.runtime.NonRestartableComposable

/**
 * Extension property to get system bars insets except bottom
 */
val WindowInsets.Companion.systemBarsIgnoringBottom
    @Composable @NonRestartableComposable get() = WindowInsets.systemBars.only(
        WindowInsetsSides.Top + WindowInsetsSides.Horizontal
    )