package mega.privacy.android.a11ytest.sweep

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.ComposeContentTestRule
import androidx.compose.ui.unit.dp
import mega.android.core.ui.theme.AndroidTheme

/**
 * Mounts sweep [content] inside the design-system theme, matching how feature modules host
 * core-ui components in production.
 */
internal fun ComposeContentTestRule.setSweepContent(
    content: @Composable ColumnScope.() -> Unit,
) {
    setContent {
        AndroidTheme(isDark = false) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                content()
            }
        }
    }
}
