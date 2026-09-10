package mega.privacy.android.a11ytest.screen

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.ComposeContentTestRule
import com.google.android.apps.common.testing.accessibility.framework.AccessibilityCheckResult
import mega.android.core.ui.components.surface.BoxSurface
import mega.android.core.ui.components.surface.SurfaceColor
import mega.android.core.ui.theme.AndroidTheme
import mega.privacy.android.a11ytest.ThemeReporters
import mega.privacy.android.a11ytest.ThemedContent
import org.hamcrest.Matcher

/**
 * Mounts a feature-module screen [content] inside the design-system theme, filling the whole
 * window so the screen's own scaffold, top bar and insets lay out exactly as in production.
 *
 * Screens are mounted stateless: the caller passes hand-built UI-state objects and no-op
 * callbacks, so no ViewModel, Hilt graph or gateway fake takes part in the render.
 *
 * The screen sits on the design system's page background rather than the host activity's window
 * background, so anything the screen leaves transparent is measured against the theme's own
 * backdrop instead of a theme-independent one.
 *
 * @param reporters receives each theme's results into its own report
 * @param baselineSuppressions known findings in the prebuilt core-ui artifact that must not
 * fail this sweep; see `megaAccessibilityValidator`
 * @return the mounted content, ready to be checked in every theme
 */
fun ComposeContentTestRule.setScreenContent(
    reporters: ThemeReporters,
    baselineSuppressions: Matcher<AccessibilityCheckResult>? = null,
    content: @Composable () -> Unit,
): ThemedContent {
    val darkTheme = mutableStateOf(false)
    setContent {
        AndroidTheme(isDark = darkTheme.value) {
            BoxSurface(
                SurfaceColor.PageBackground,
                Modifier.fillMaxSize(),
            ) {
                content()
            }
        }
    }
    return ThemedContent(this, darkTheme, reporters, baselineSuppressions)
}
