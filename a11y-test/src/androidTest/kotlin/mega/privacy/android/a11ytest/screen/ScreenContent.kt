package mega.privacy.android.a11ytest.screen

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.ComposeContentTestRule
import mega.android.core.ui.theme.AndroidTheme

/**
 * Mounts a feature-module screen [content] inside the design-system theme, filling the whole
 * window so the screen's own scaffold, top bar and insets lay out exactly as in production.
 *
 * Screens are mounted stateless: the caller passes hand-built UI-state objects and no-op
 * callbacks, so no ViewModel, Hilt graph or gateway fake takes part in the render.
 */
internal fun ComposeContentTestRule.setScreenContent(
    content: @Composable () -> Unit,
) {
    setContent {
        AndroidTheme(isDark = false) {
            Box(modifier = Modifier.fillMaxSize()) {
                content()
            }
        }
    }
}
