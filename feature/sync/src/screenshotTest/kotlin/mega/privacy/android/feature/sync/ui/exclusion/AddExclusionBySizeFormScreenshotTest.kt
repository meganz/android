package mega.privacy.android.feature.sync.ui.exclusion

import androidx.compose.runtime.Composable
import com.android.tools.screenshot.PreviewTest
import mega.android.core.ui.preview.CombinedThemePreviews
import mega.android.core.ui.theme.AndroidThemeForPreviews
import mega.privacy.android.feature.sync.ui.model.exclusion.AddExclusionBySizeFormState
import mega.privacy.android.feature.sync.ui.model.exclusion.SyncSizeBoundInput
import mega.privacy.android.feature.sync.ui.model.exclusion.SyncSizeCondition
import mega.privacy.android.feature.sync.ui.model.exclusion.SyncSizeUnit

/**
 * One preview per distinct layout the form has: no bounds, one bound, a range whose two bounds
 * carry different units, and that range in error. A second single-bound condition renders the
 * same layout with a different chip label, which the chip's own tests already cover.
 */
class AddExclusionBySizeFormScreenshotTest {

    @PreviewTest
    @CombinedThemePreviews
    @Composable
    fun AddExclusionBySizeFormEmpty() {
        AndroidThemeForPreviews {
            Form(AddExclusionBySizeFormState())
        }
    }


    @PreviewTest
    @CombinedThemePreviews
    @Composable
    fun AddExclusionBySizeFormBiggerThanGigabytes() {
        AndroidThemeForPreviews {
            Form(
                AddExclusionBySizeFormState(
                    condition = SyncSizeCondition.BiggerThan,
                    firstBound = SyncSizeBoundInput(value = "1", unit = SyncSizeUnit.Gigabytes),
                )
            )
        }
    }



    /** The pair the shared-unit design could not express: each bound in a different unit. */
    @PreviewTest
    @CombinedThemePreviews
    @Composable
    fun AddExclusionBySizeFormNotBetweenMixedUnits() {
        AndroidThemeForPreviews {
            Form(
                AddExclusionBySizeFormState(
                    condition = SyncSizeCondition.NotBetween,
                    firstBound = SyncSizeBoundInput(value = "1", unit = SyncSizeUnit.Kilobytes),
                    secondBound = SyncSizeBoundInput(value = "2", unit = SyncSizeUnit.Gigabytes),
                )
            )
        }
    }

    /** The one error state the design draws: a range typed the wrong way round. */
    @PreviewTest
    @CombinedThemePreviews
    @Composable
    fun AddExclusionBySizeFormNotBetweenInvertedRange() {
        AndroidThemeForPreviews {
            Form(
                AddExclusionBySizeFormState(
                    condition = SyncSizeCondition.NotBetween,
                    firstBound = SyncSizeBoundInput(value = "2", unit = SyncSizeUnit.Gigabytes),
                    secondBound = SyncSizeBoundInput(value = "1", unit = SyncSizeUnit.Kilobytes),
                )
            )
        }
    }

    @Composable
    private fun Form(state: AddExclusionBySizeFormState) {
        AddExclusionBySizeForm(
            state = state,
            onConditionSelected = {},
            onFirstBoundValueChanged = {},
            onFirstBoundUnitSelected = {},
            onSecondBoundValueChanged = {},
            onSecondBoundUnitSelected = {},
        )
    }
}
