package mega.privacy.android.shared.nodes.components

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import com.android.tools.screenshot.PreviewTest
import mega.android.core.ui.preview.CombinedThemePreviews
import mega.android.core.ui.theme.AndroidThemeForPreviews

class NodesViewSkeletonScreenshotTest {

    @PreviewTest
    @CombinedThemePreviews
    @Composable
    fun NodesViewSkeletonList() {
        AndroidThemeForPreviews {
            NodeListViewSkeleton(contentPadding = PaddingValues(top = 4.dp))
        }
    }

    @PreviewTest
    @CombinedThemePreviews
    @Composable
    fun NodesViewSkeletonGrid() {
        AndroidThemeForPreviews {
            NodeGridViewSkeleton(contentPadding = PaddingValues(top = 4.dp))
        }
    }
}
