package mega.privacy.mobile.home.presentation.home.widget.chips

import androidx.compose.runtime.Composable
import com.android.tools.screenshot.PreviewTest
import mega.android.core.ui.preview.CombinedThemePreviews
import mega.android.core.ui.theme.AndroidThemeForPreviews

/**
 * Screenshot tests for the Home shortcuts chips row, covering the full row and the
 * variant where chips are hidden because their section is in the bottom navigation bar.
 */
class HomeChipsWidgetScreenshotTest {

    @PreviewTest
    @CombinedThemePreviews
    @Composable
    fun HomeChipsAllSections() {
        AndroidThemeForPreviews {
            HomeChips(
                hiddenSectionIds = emptySet(),
                onNavigate = {},
            )
        }
    }

    @PreviewTest
    @CombinedThemePreviews
    @Composable
    fun HomeChipsFavouritesAndOfflineHidden() {
        AndroidThemeForPreviews {
            HomeChips(
                hiddenSectionIds = setOf(
                    HomeChipSectionIds.FAVOURITES,
                    HomeChipSectionIds.OFFLINE,
                ),
                onNavigate = {},
            )
        }
    }
}
