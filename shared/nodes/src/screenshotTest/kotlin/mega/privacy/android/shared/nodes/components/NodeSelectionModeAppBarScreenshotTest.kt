package mega.privacy.android.shared.nodes.components

import androidx.compose.runtime.Composable
import com.android.tools.screenshot.PreviewTest
import mega.android.core.ui.preview.CombinedThemePreviews
import mega.android.core.ui.theme.AndroidThemeForPreviews

class NodeSelectionModeAppBarScreenshotTest {

    @PreviewTest
    @CombinedThemePreviews
    @Composable
    fun NodeSelectionModeAppBarCount() {
        AndroidThemeForPreviews {
            NodeSelectionModeAppBar(
                count = 3,
                isAllSelected = false,
                isSelecting = false,
                onSelectAllClicked = {},
                onCancelSelectionClicked = {},
            )
        }
    }

    @PreviewTest
    @CombinedThemePreviews
    @Composable
    fun NodeSelectionModeAppBarCountOfTotal() {
        AndroidThemeForPreviews {
            NodeSelectionModeAppBar(
                count = 500,
                isAllSelected = true,
                isSelecting = false,
                onSelectAllClicked = {},
                onCancelSelectionClicked = {},
                total = 213456,
            )
        }
    }

    @PreviewTest
    @CombinedThemePreviews
    @Composable
    fun NodeSelectionModeAppBarSelecting() {
        AndroidThemeForPreviews {
            NodeSelectionModeAppBar(
                count = 500,
                isAllSelected = false,
                isSelecting = true,
                onSelectAllClicked = {},
                onCancelSelectionClicked = {},
            )
        }
    }
}
