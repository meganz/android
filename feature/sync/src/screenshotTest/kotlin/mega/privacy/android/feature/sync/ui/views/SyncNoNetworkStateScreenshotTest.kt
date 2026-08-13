package mega.privacy.android.feature.sync.ui.views

import androidx.compose.runtime.Composable
import com.android.tools.screenshot.PreviewTest
import mega.android.core.ui.preview.CombinedThemePreviews
import mega.android.core.ui.theme.AndroidThemeForPreviews

/**
 * Covers the M3 scaffold and top app bar this screen moved onto, which the sync list itself
 * cannot exercise in a screenshot test because it is driven by view models.
 */
class SyncNoNetworkStateScreenshotTest {

    @PreviewTest
    @CombinedThemePreviews
    @Composable
    fun SyncNoNetworkStateDefault() {
        AndroidThemeForPreviews {
            SyncNoNetworkState(onBackPressed = {})
        }
    }
}
