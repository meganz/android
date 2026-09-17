package mega.privacy.android.feature.sync.ui.exclusion

import androidx.compose.runtime.Composable
import com.android.tools.screenshot.PreviewTest
import mega.android.core.ui.preview.CombinedThemePreviews
import mega.android.core.ui.theme.AndroidThemeForPreviews
import mega.privacy.android.feature.sync.ui.model.exclusion.AddExclusionBySizeFormState
import mega.privacy.android.feature.sync.ui.model.exclusion.EditSyncSizeExclusionUiState
import mega.privacy.android.feature.sync.ui.model.exclusion.SyncSizeBoundInput
import mega.privacy.android.feature.sync.ui.model.exclusion.SyncSizeCondition
import mega.privacy.android.feature.sync.ui.model.exclusion.SyncSizeUnit

/**
 * The Edit chrome around the by-size form. One bound and a range are pinned, because the range
 * is the only state that draws the second bound and the conjunction.
 */
class EditSyncSizeExclusionScreenScreenshotTest {

    @PreviewTest
    @CombinedThemePreviews
    @Composable
    fun EditSyncSizeExclusionScreenSingleBound() {
        AndroidThemeForPreviews {
            Screen(
                EditSyncSizeExclusionUiState(
                    bySize = AddExclusionBySizeFormState(
                        condition = SyncSizeCondition.BiggerThan,
                        firstBound = SyncSizeBoundInput(
                            value = "100",
                            unit = SyncSizeUnit.Megabytes,
                        ),
                    ),
                )
            )
        }
    }

    @PreviewTest
    @CombinedThemePreviews
    @Composable
    fun EditSyncSizeExclusionScreenRange() {
        AndroidThemeForPreviews {
            Screen(
                EditSyncSizeExclusionUiState(
                    bySize = AddExclusionBySizeFormState(
                        condition = SyncSizeCondition.NotBetween,
                        firstBound = SyncSizeBoundInput(value = "1", unit = SyncSizeUnit.Kilobytes),
                        secondBound = SyncSizeBoundInput(
                            value = "2",
                            unit = SyncSizeUnit.Gigabytes,
                        ),
                    ),
                )
            )
        }
    }

    @Composable
    private fun Screen(uiState: EditSyncSizeExclusionUiState) {
        EditSyncSizeExclusionScreen(
            uiState = uiState,
            onConditionSelected = {},
            onFirstBoundValueChanged = {},
            onFirstBoundUnitSelected = {},
            onSecondBoundValueChanged = {},
            onSecondBoundUnitSelected = {},
            onDelete = {},
            onSave = {},
            onDismiss = {},
        )
    }
}
