package mega.privacy.android.feature.sync.ui.synclist

import androidx.compose.runtime.Composable
import com.android.tools.screenshot.PreviewTest
import mega.android.core.ui.preview.CombinedThemePreviews
import mega.android.core.ui.theme.AndroidThemeForPreviews

/**
 * The chip row is the only part of SyncListScreen that is not view model driven, so it is the one
 * piece of the shell that can be pinned. Its spacing regressed unnoticed in the M3 migration.
 */
class HeaderChipsScreenshotTest {

    @PreviewTest
    @CombinedThemePreviews
    @Composable
    fun HeaderChipsFoldersSelected() {
        AndroidThemeForPreviews {
            HeaderChips(
                selectedChip = SyncChip.SYNC_FOLDERS,
                stalledIssuesCount = 0,
                onChipSelected = {},
            )
        }
    }

    @PreviewTest
    @CombinedThemePreviews
    @Composable
    fun HeaderChipsStalledIssuesSelectedWithCount() {
        AndroidThemeForPreviews {
            HeaderChips(
                selectedChip = SyncChip.STALLED_ISSUES,
                stalledIssuesCount = 3,
                onChipSelected = {},
            )
        }
    }
}
