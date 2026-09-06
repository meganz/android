package mega.privacy.android.a11ytest.sweep

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.ComposeContentTestRule
import androidx.compose.ui.unit.dp
import com.google.android.apps.common.testing.accessibility.framework.AccessibilityCheckResult
import mega.android.core.ui.components.surface.ColumnSurface
import mega.android.core.ui.components.surface.SurfaceColor
import mega.android.core.ui.theme.AndroidTheme
import mega.privacy.android.a11ytest.ThemeReporters
import mega.privacy.android.a11ytest.ThemedContent
import org.hamcrest.Matcher

/**
 * Mounts sweep [content] inside the design-system theme, matching how feature modules host
 * core-ui components in production.
 *
 * The content sits on the design system's page background rather than the host activity's
 * window background: contrast is measured against the pixels actually behind the component, so
 * a theme-independent window background would make the dark sweep measure dark-theme
 * foregrounds against a light backdrop.
 *
 * @param reporters receives each theme's results into its own report
 * @param baselineSuppressions known findings in the prebuilt core-ui artifact that must not
 * fail this sweep; see `megaAccessibilityValidator`
 * @return the mounted content, ready to be checked in every theme
 */
internal fun ComposeContentTestRule.setSweepContent(
    reporters: ThemeReporters,
    baselineSuppressions: Matcher<AccessibilityCheckResult>? = null,
    content: @Composable ColumnScope.() -> Unit,
): ThemedContent {
    val darkTheme = mutableStateOf(false)
    setContent {
        AndroidTheme(isDark = darkTheme.value) {
            ColumnSurface(
                SurfaceColor.PageBackground,
                Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                Arrangement.spacedBy(12.dp),
            ) {
                content()
            }
        }
    }
    return ThemedContent(this, darkTheme, reporters, baselineSuppressions)
}
