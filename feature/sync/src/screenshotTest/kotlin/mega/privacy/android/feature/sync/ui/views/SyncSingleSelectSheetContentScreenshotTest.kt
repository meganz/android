package mega.privacy.android.feature.sync.ui.views

import androidx.compose.runtime.Composable
import com.android.tools.screenshot.PreviewTest
import mega.android.core.ui.preview.CombinedThemePreviews
import mega.android.core.ui.theme.AndroidThemeForPreviews

/**
 * The sheet body is pinned rather than [SyncSingleSelectBottomSheet] itself, because the modal
 * sheet needs a real window that the screenshot harness does not provide.
 */
class SyncSingleSelectSheetContentScreenshotTest {

    @PreviewTest
    @CombinedThemePreviews
    @Composable
    fun SyncSingleSelectSheetContentWithSelection() {
        AndroidThemeForPreviews {
            SyncSingleSelectSheetContent(
                title = "Unit",
                options = SIZE_UNITS,
                selectedValue = "KB",
                onOptionSelected = {},
            )
        }
    }

    @PreviewTest
    @CombinedThemePreviews
    @Composable
    fun SyncSingleSelectSheetContentWithSubtitle() {
        AndroidThemeForPreviews {
            SyncSingleSelectSheetContent(
                title = "Condition",
                subtitle = "Applies to the file size",
                options = SIZE_CONDITIONS,
                selectedValue = "BIGGER_THAN",
                onOptionSelected = {},
            )
        }
    }

    @PreviewTest
    @CombinedThemePreviews
    @Composable
    fun SyncSingleSelectSheetContentWithoutSelection() {
        AndroidThemeForPreviews {
            SyncSingleSelectSheetContent(
                title = "Unit",
                options = SIZE_UNITS,
                selectedValue = null,
                onOptionSelected = {},
            )
        }
    }

    private companion object {
        val SIZE_UNITS = listOf(
            SyncSingleSelectOption("B", "Bytes"),
            SyncSingleSelectOption("KB", "Kilobytes"),
            SyncSingleSelectOption("MB", "Megabytes"),
            SyncSingleSelectOption("GB", "Gigabytes"),
        )

        val SIZE_CONDITIONS = listOf(
            SyncSingleSelectOption("BIGGER_THAN", "Bigger than"),
            SyncSingleSelectOption("SMALLER_THAN", "Smaller than"),
            SyncSingleSelectOption("NOT_BETWEEN", "Not between"),
        )
    }
}
